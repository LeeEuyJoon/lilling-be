package luti.server.application.bus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import luti.server.application.command.ICommand;
import luti.server.application.command.ShortenUrlCommand;
import luti.server.application.handler.CommandHandler;
import luti.server.application.result.ShortenUrlResult;

@DisplayName("CommandBus 단위 테스트")
class CommandBusTest {

	// -------------------------------------------------------------------------
	// 내부 테스트용 Command/Handler 스텁 클래스
	// -------------------------------------------------------------------------

	static class AnotherCommand implements ICommand<String> {
		private final String value;

		AnotherCommand(String value) {
			this.value = value;
		}

		String getValue() {
			return value;
		}
	}

	static class AnotherCommandHandler implements CommandHandler<AnotherCommand, String> {

		@Override
		public String execute(AnotherCommand command) {
			return "handled: " + command.getValue();
		}

		@Override
		public Class<AnotherCommand> getSupportedCommandType() {
			return AnotherCommand.class;
		}
	}

	static class UnregisteredCommand implements ICommand<String> {}

	// -------------------------------------------------------------------------
	// 라우팅 검증
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("핸들러 라우팅 검증")
	class HandlerRouting {

		@Test
		@DisplayName("ShortenUrlCommand - 등록된 핸들러로 올바르게 라우팅됨")
		void ShortenUrlCommand_등록된핸들러_라우팅() {
			// Given
			String originalUrl = "https://www.example.com";
			ShortenUrlCommand command = new ShortenUrlCommand(null, originalUrl, null);
			ShortenUrlResult expected = ShortenUrlResult.of("https://lill.ing/abc1234");

			@SuppressWarnings("unchecked")
			CommandHandler<ShortenUrlCommand, ShortenUrlResult> mockHandler =
				(CommandHandler<ShortenUrlCommand, ShortenUrlResult>) mock(CommandHandler.class);
			when(mockHandler.getSupportedCommandType()).thenReturn(ShortenUrlCommand.class);
			when(mockHandler.execute(command)).thenReturn(expected);

			CommandBus commandBus = new CommandBus(List.of(mockHandler));

			System.out.println("=== ShortenUrlCommand 라우팅 검증 ===");
			System.out.println("Command 타입: " + command.getClass().getSimpleName());

			// When
			ShortenUrlResult result = commandBus.execute(command);

			// Then
			assertNotNull(result);
			assertEquals(expected.getShortenedUrl(), result.getShortenedUrl());

			System.out.println("라우팅 성공, shortUrl: " + result.getShortenedUrl());

			verify(mockHandler).execute(command);
		}

		@Test
		@DisplayName("AnotherCommand - 해당 타입의 핸들러로 올바르게 라우팅됨")
		void AnotherCommand_등록된핸들러_라우팅() {
			// Given
			AnotherCommand command = new AnotherCommand("test-value");
			AnotherCommandHandler anotherHandler = new AnotherCommandHandler();
			CommandBus commandBus = new CommandBus(List.of(anotherHandler));

			System.out.println("=== AnotherCommand 라우팅 검증 ===");
			System.out.println("Command 타입: " + command.getClass().getSimpleName());
			System.out.println("입력값: " + command.getValue());

			// When
			String result = commandBus.execute(command);

			// Then
			assertNotNull(result);
			assertEquals("handled: test-value", result);

			System.out.println("라우팅 성공, 결과: " + result);
		}

		@Test
		@DisplayName("여러 핸들러 등록 - 각 Command가 올바른 핸들러로 라우팅됨")
		void 여러핸들러_각Command_올바른핸들러_라우팅() {
			// Given
			ShortenUrlResult shortenResult = ShortenUrlResult.of("https://lill.ing/abc1234");

			@SuppressWarnings("unchecked")
			CommandHandler<ShortenUrlCommand, ShortenUrlResult> shortenHandler =
				(CommandHandler<ShortenUrlCommand, ShortenUrlResult>) mock(CommandHandler.class);
			when(shortenHandler.getSupportedCommandType()).thenReturn(ShortenUrlCommand.class);
			when(shortenHandler.execute(any(ShortenUrlCommand.class))).thenReturn(shortenResult);

			AnotherCommandHandler anotherHandler = new AnotherCommandHandler();

			CommandBus commandBus = new CommandBus(List.of(shortenHandler, anotherHandler));

			ShortenUrlCommand shortenCommand = new ShortenUrlCommand(null, "https://example.com", null);
			AnotherCommand anotherCommand = new AnotherCommand("hello");

			System.out.println("=== 여러 핸들러 라우팅 검증 ===");

			// When
			ShortenUrlResult resultA = commandBus.execute(shortenCommand);
			String resultB = commandBus.execute(anotherCommand);

			// Then
			assertNotNull(resultA);
			assertEquals("https://lill.ing/abc1234", resultA.getShortenedUrl());

			assertNotNull(resultB);
			assertEquals("handled: hello", resultB);

			System.out.println("ShortenUrlCommand 결과: " + resultA.getShortenedUrl());
			System.out.println("AnotherCommand 결과: " + resultB);

			verify(shortenHandler).execute(shortenCommand);
		}

		@Test
		@DisplayName("같은 Command 타입으로 여러 번 실행 - 매번 동일한 핸들러가 처리")
		void 같은Command_여러번실행_동일핸들러_처리() {
			// Given
			AnotherCommandHandler handler = spy(new AnotherCommandHandler());
			CommandBus commandBus = new CommandBus(List.of(handler));

			System.out.println("=== 같은 Command 타입 반복 실행 검증 ===");

			// When
			String result1 = commandBus.execute(new AnotherCommand("first"));
			String result2 = commandBus.execute(new AnotherCommand("second"));
			String result3 = commandBus.execute(new AnotherCommand("third"));

			// Then
			assertEquals("handled: first", result1);
			assertEquals("handled: second", result2);
			assertEquals("handled: third", result3);

			System.out.println("결과1: " + result1);
			System.out.println("결과2: " + result2);
			System.out.println("결과3: " + result3);

			verify(handler, times(3)).execute(any(AnotherCommand.class));
		}
	}

	// -------------------------------------------------------------------------
	// 미등록 Command 예외 검증
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("미등록 Command 예외 검증")
	class UnregisteredCommandException {

		@Test
		@DisplayName("미등록 Command 실행 - IllegalArgumentException 발생")
		void 미등록Command_실행_예외발생() {
			// Given
			AnotherCommandHandler anotherHandler = new AnotherCommandHandler();
			CommandBus commandBus = new CommandBus(List.of(anotherHandler));

			UnregisteredCommand unregisteredCommand = new UnregisteredCommand();

			System.out.println("=== 미등록 Command 예외 검증 ===");
			System.out.println("미등록 Command 타입: " + unregisteredCommand.getClass().getSimpleName());

			// When & Then
			IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> commandBus.execute(unregisteredCommand));

			assertNotNull(exception.getMessage());
			assertTrue(exception.getMessage().contains("UnregisteredCommand"),
				"예외 메시지에 Command 클래스명이 포함되어야 함. 실제: " + exception.getMessage());

			System.out.println("예외 발생: " + exception.getMessage());
		}

		@Test
		@DisplayName("핸들러 없이 생성된 CommandBus - 모든 Command에 대해 IllegalArgumentException 발생")
		void 핸들러없는_CommandBus_모든Command_예외발생() {
			// Given
			CommandBus emptyCommandBus = new CommandBus(List.of());

			ShortenUrlCommand command = new ShortenUrlCommand(null, "https://example.com", null);

			System.out.println("=== 빈 CommandBus 예외 검증 ===");

			// When & Then
			IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> emptyCommandBus.execute(command));

			assertNotNull(exception.getMessage());
			assertTrue(exception.getMessage().contains("ShortenUrlCommand"),
				"예외 메시지에 Command 클래스명이 포함되어야 함. 실제: " + exception.getMessage());

			System.out.println("예외 발생: " + exception.getMessage());
		}

		@Test
		@DisplayName("ShortenUrlCommand 핸들러만 등록 - AnotherCommand 실행 시 IllegalArgumentException")
		void ShortenUrlCommand핸들러만_등록_AnotherCommand_예외() {
			// Given
			@SuppressWarnings("unchecked")
			CommandHandler<ShortenUrlCommand, ShortenUrlResult> shortenHandler =
				(CommandHandler<ShortenUrlCommand, ShortenUrlResult>) mock(CommandHandler.class);
			when(shortenHandler.getSupportedCommandType()).thenReturn(ShortenUrlCommand.class);

			CommandBus commandBus = new CommandBus(List.of(shortenHandler));

			AnotherCommand unregistered = new AnotherCommand("test");

			System.out.println("=== ShortenUrlCommand 핸들러만 있을 때 AnotherCommand 예외 ===");

			// When & Then
			IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> commandBus.execute(unregistered));

			assertNotNull(exception.getMessage());

			System.out.println("예외 발생: " + exception.getMessage());
		}
	}

	// -------------------------------------------------------------------------
	// CommandBus 생성 시 핸들러 등록 검증
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("CommandBus 생성 - 핸들러 등록 검증")
	class CommandBusConstruction {

		@Test
		@DisplayName("중복 타입의 핸들러 등록 - 마지막 핸들러로 덮어써짐")
		void 중복타입_핸들러_등록_마지막핸들러_덮어씀() {
			// Given
			AnotherCommandHandler handler1 = new AnotherCommandHandler() {
				@Override
				public String execute(AnotherCommand command) {
					return "handler1: " + command.getValue();
				}
			};
			AnotherCommandHandler handler2 = new AnotherCommandHandler() {
				@Override
				public String execute(AnotherCommand command) {
					return "handler2: " + command.getValue();
				}
			};

			// handler2가 나중에 등록되므로 handler1을 덮어씀
			CommandBus commandBus = new CommandBus(List.of(handler1, handler2));

			AnotherCommand command = new AnotherCommand("input");

			System.out.println("=== 중복 핸들러 등록 시 덮어쓰기 동작 검증 ===");

			// When
			String result = commandBus.execute(command);

			// Then
			// HashMap에 마지막으로 put된 핸들러가 사용됨
			assertEquals("handler2: input", result);

			System.out.println("실행된 핸들러 결과: " + result);
		}
	}
}
