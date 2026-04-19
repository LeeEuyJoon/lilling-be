package luti.server.application.handler.command;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import luti.server.application.command.ShortenUrlCommand;
import luti.server.application.result.ShortenUrlResult;
import luti.server.domain.service.UrlShorteningService;
import luti.server.domain.util.Base62Encoder;
import luti.server.domain.util.IdScrambler;
import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;
import luti.server.infrastructure.client.kgs.KeyBlockManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShortenUrlCommandHandler - execute() 단위 테스트")
class ShortenUrlCommandHandlerTest {

	@Mock
	private IdScrambler idScrambler;

	@Mock
	private Base62Encoder base62Encoder;

	@Mock
	private KeyBlockManager keyBlockManager;

	@Mock
	private UrlShorteningService urlShorteningService;

	private ShortenUrlCommandHandler handler;

	@BeforeEach
	void setUp() {
		handler = new ShortenUrlCommandHandler(
			idScrambler,
			base62Encoder,
			keyBlockManager,
			urlShorteningService
		);
	}

	// -------------------------------------------------------------------------
	// Auto 단축 (keyword 없음)
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("Auto 단축 - keyword 없음")
	class AutoShorten {

		@Test
		@DisplayName("Auto 단축 성공 - keyword null, 첫 번째 시도에서 성공")
		void auto_단축_keyword_null_첫시도_성공() {
			// Given
			String originalUrl = "https://www.example.com";
			ShortenUrlCommand command = new ShortenUrlCommand(null, originalUrl, null);

			long kgsId = 26001L;
			long scrambledId = 99999L;
			String encodedValue = "abc1234";
			String expectedShortUrl = "https://lill.ing/" + encodedValue;

			when(keyBlockManager.getNextId()).thenReturn(kgsId);
			when(idScrambler.scramble(kgsId)).thenReturn(scrambledId);
			when(base62Encoder.encode(scrambledId)).thenReturn(encodedValue);
			when(urlShorteningService.generateShortenedUrl(originalUrl, kgsId, scrambledId, encodedValue, null))
				.thenReturn(Optional.of(expectedShortUrl));

			System.out.println("=== Auto 단축 성공 (keyword null) ===");
			System.out.println("originalUrl: " + originalUrl);

			// When
			ShortenUrlResult result = handler.execute(command);

			// Then
			assertNotNull(result);
			assertEquals(expectedShortUrl, result.getShortenedUrl());

			System.out.println("shortUrl: " + result.getShortenedUrl());

			verify(urlShorteningService).validateOriginalUrl(originalUrl);
			verify(keyBlockManager).getNextId();
			verify(idScrambler).scramble(kgsId);
			verify(base62Encoder).encode(scrambledId);
			verify(urlShorteningService).generateShortenedUrl(originalUrl, kgsId, scrambledId, encodedValue, null);
		}

		@Test
		@DisplayName("Auto 단축 성공 - keyword 빈 문자열, 첫 번째 시도에서 성공")
		void auto_단축_keyword_빈문자열_첫시도_성공() {
			// Given
			String originalUrl = "https://www.naver.com";
			ShortenUrlCommand command = new ShortenUrlCommand(null, originalUrl, "");

			long kgsId = 26002L;
			long scrambledId = 88888L;
			String encodedValue = "xyz5678";
			String expectedShortUrl = "https://lill.ing/" + encodedValue;

			when(keyBlockManager.getNextId()).thenReturn(kgsId);
			when(idScrambler.scramble(kgsId)).thenReturn(scrambledId);
			when(base62Encoder.encode(scrambledId)).thenReturn(encodedValue);
			when(urlShorteningService.generateShortenedUrl(originalUrl, kgsId, scrambledId, encodedValue, null))
				.thenReturn(Optional.of(expectedShortUrl));

			System.out.println("=== Auto 단축 성공 (keyword 빈 문자열) ===");
			System.out.println("originalUrl: " + originalUrl);

			// When
			ShortenUrlResult result = handler.execute(command);

			// Then
			assertNotNull(result);
			assertEquals(expectedShortUrl, result.getShortenedUrl());

			System.out.println("shortUrl: " + result.getShortenedUrl());
		}

		@Test
		@DisplayName("Auto 단축 성공 - keyword 공백만, 첫 번째 시도에서 성공")
		void auto_단축_keyword_공백만_첫시도_성공() {
			// Given
			String originalUrl = "https://www.github.com";
			ShortenUrlCommand command = new ShortenUrlCommand(null, originalUrl, "   ");

			long kgsId = 26003L;
			long scrambledId = 77777L;
			String encodedValue = "def9012";
			String expectedShortUrl = "https://lill.ing/" + encodedValue;

			when(keyBlockManager.getNextId()).thenReturn(kgsId);
			when(idScrambler.scramble(kgsId)).thenReturn(scrambledId);
			when(base62Encoder.encode(scrambledId)).thenReturn(encodedValue);
			when(urlShorteningService.generateShortenedUrl(originalUrl, kgsId, scrambledId, encodedValue, null))
				.thenReturn(Optional.of(expectedShortUrl));

			System.out.println("=== Auto 단축 성공 (keyword 공백만) ===");

			// When
			ShortenUrlResult result = handler.execute(command);

			// Then
			assertNotNull(result);
			assertEquals(expectedShortUrl, result.getShortenedUrl());

			System.out.println("shortUrl: " + result.getShortenedUrl());
		}

		@Test
		@DisplayName("Auto 단축 성공 - 첫 번째 시도 실패, 두 번째 시도에서 성공")
		void auto_단축_첫시도_실패_두번째_성공() {
			// Given
			String originalUrl = "https://www.example.com/retry";
			ShortenUrlCommand command = new ShortenUrlCommand(null, originalUrl, null);

			long kgsId1 = 26001L;
			long kgsId2 = 26002L;
			long scrambledId1 = 11111L;
			long scrambledId2 = 22222L;
			String encoded1 = "aaaaaa1";
			String encoded2 = "bbbbb22";
			String expectedShortUrl = "https://lill.ing/" + encoded2;

			when(keyBlockManager.getNextId())
				.thenReturn(kgsId1)
				.thenReturn(kgsId2);
			when(idScrambler.scramble(kgsId1)).thenReturn(scrambledId1);
			when(idScrambler.scramble(kgsId2)).thenReturn(scrambledId2);
			when(base62Encoder.encode(scrambledId1)).thenReturn(encoded1);
			when(base62Encoder.encode(scrambledId2)).thenReturn(encoded2);
			when(urlShorteningService.generateShortenedUrl(originalUrl, kgsId1, scrambledId1, encoded1, null))
				.thenReturn(Optional.empty());
			when(urlShorteningService.generateShortenedUrl(originalUrl, kgsId2, scrambledId2, encoded2, null))
				.thenReturn(Optional.of(expectedShortUrl));

			System.out.println("=== Auto 단축 - 첫 번째 실패, 두 번째 성공 ===");
			System.out.println("originalUrl: " + originalUrl);

			// When
			ShortenUrlResult result = handler.execute(command);

			// Then
			assertNotNull(result);
			assertEquals(expectedShortUrl, result.getShortenedUrl());

			System.out.println("shortUrl: " + result.getShortenedUrl());

			verify(keyBlockManager, times(2)).getNextId();
			verify(urlShorteningService, times(2)).generateShortenedUrl(any(), anyLong(), anyLong(), anyString(), any());
		}

		@Test
		@DisplayName("Auto 단축 실패 - 최대 재시도 횟수(20회) 모두 실패, AUTO_SHORTEN_FAILED 예외")
		void auto_단축_최대재시도_모두실패_예외() {
			// Given
			String originalUrl = "https://www.example.com/fail";
			ShortenUrlCommand command = new ShortenUrlCommand(null, originalUrl, null);

			when(keyBlockManager.getNextId()).thenReturn(26001L);
			when(idScrambler.scramble(anyLong())).thenReturn(99999L);
			when(base62Encoder.encode(anyLong())).thenReturn("abc1234");
			when(urlShorteningService.generateShortenedUrl(any(), anyLong(), anyLong(), anyString(), any()))
				.thenReturn(Optional.empty());

			System.out.println("=== Auto 단축 최대 재시도 실패 ===");
			System.out.println("originalUrl: " + originalUrl);

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> handler.execute(command));

			assertEquals(ErrorCode.AUTO_SHORTEN_FAILED, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			// 20번 모두 시도
			verify(keyBlockManager, times(20)).getNextId();
			verify(urlShorteningService, times(20))
				.generateShortenedUrl(any(), anyLong(), anyLong(), anyString(), any());
		}

		@Test
		@DisplayName("Auto 단축 성공 - memberId 있음, generateShortenedUrl에 memberId 전달")
		void auto_단축_memberId_있음_전달() {
			// Given
			Long memberId = 42L;
			String originalUrl = "https://www.example.com/auth";
			ShortenUrlCommand command = new ShortenUrlCommand(memberId, originalUrl, null);

			long kgsId = 26001L;
			long scrambledId = 55555L;
			String encodedValue = "mem1234";
			String expectedShortUrl = "https://lill.ing/" + encodedValue;

			when(keyBlockManager.getNextId()).thenReturn(kgsId);
			when(idScrambler.scramble(kgsId)).thenReturn(scrambledId);
			when(base62Encoder.encode(scrambledId)).thenReturn(encodedValue);
			when(urlShorteningService.generateShortenedUrl(originalUrl, kgsId, scrambledId, encodedValue, memberId))
				.thenReturn(Optional.of(expectedShortUrl));

			System.out.println("=== Auto 단축 - memberId 전달 검증 ===");
			System.out.println("memberId: " + memberId);
			System.out.println("originalUrl: " + originalUrl);

			// When
			ShortenUrlResult result = handler.execute(command);

			// Then
			assertNotNull(result);
			assertEquals(expectedShortUrl, result.getShortenedUrl());

			System.out.println("shortUrl: " + result.getShortenedUrl());

			verify(urlShorteningService).generateShortenedUrl(originalUrl, kgsId, scrambledId, encodedValue, memberId);
		}
	}

	// -------------------------------------------------------------------------
	// Keyword 단축 (keyword 있음)
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("Keyword 단축 - keyword 있음")
	class KeywordShorten {

		@Test
		@DisplayName("Keyword 단축 성공 - 유효한 keyword로 단축 성공")
		void keyword_단축_성공() {
			// Given
			String originalUrl = "https://www.example.com/keyword";
			String keyword = "mylink";
			ShortenUrlCommand command = new ShortenUrlCommand(null, originalUrl, keyword);

			String expectedShortUrl = "https://lill.ing/" + keyword;

			when(urlShorteningService.generateShortenedUrlWithKeyword(originalUrl, keyword, null))
				.thenReturn(expectedShortUrl);

			System.out.println("=== Keyword 단축 성공 ===");
			System.out.println("keyword: " + keyword);
			System.out.println("originalUrl: " + originalUrl);

			// When
			ShortenUrlResult result = handler.execute(command);

			// Then
			assertNotNull(result);
			assertEquals(expectedShortUrl, result.getShortenedUrl());

			System.out.println("shortUrl: " + result.getShortenedUrl());

			verify(urlShorteningService).validateOriginalUrl(originalUrl);
			verify(urlShorteningService).generateShortenedUrlWithKeyword(originalUrl, keyword, null);
			verifyNoInteractions(keyBlockManager);
			verifyNoInteractions(idScrambler);
			verifyNoInteractions(base62Encoder);
		}

		@Test
		@DisplayName("Keyword 단축 성공 - memberId 있음, generateShortenedUrlWithKeyword에 memberId 전달")
		void keyword_단축_memberId_있음_전달() {
			// Given
			Long memberId = 99L;
			String originalUrl = "https://www.example.com/auth-keyword";
			String keyword = "authkw";
			ShortenUrlCommand command = new ShortenUrlCommand(memberId, originalUrl, keyword);

			String expectedShortUrl = "https://lill.ing/" + keyword;

			when(urlShorteningService.generateShortenedUrlWithKeyword(originalUrl, keyword, memberId))
				.thenReturn(expectedShortUrl);

			System.out.println("=== Keyword 단축 - memberId 전달 검증 ===");
			System.out.println("memberId: " + memberId);
			System.out.println("keyword: " + keyword);

			// When
			ShortenUrlResult result = handler.execute(command);

			// Then
			assertNotNull(result);
			assertEquals(expectedShortUrl, result.getShortenedUrl());

			System.out.println("shortUrl: " + result.getShortenedUrl());

			verify(urlShorteningService).generateShortenedUrlWithKeyword(originalUrl, keyword, memberId);
		}

		@Test
		@DisplayName("Keyword 단축 실패 - 이미 존재하는 keyword, CANNOT_USE_KEYWORD 예외 전파")
		void keyword_단축_이미존재_예외_전파() {
			// Given
			String originalUrl = "https://www.example.com/dup";
			String keyword = "abcdefg"; // 7글자 - 충돌 시 즉시 실패
			ShortenUrlCommand command = new ShortenUrlCommand(null, originalUrl, keyword);

			when(urlShorteningService.generateShortenedUrlWithKeyword(originalUrl, keyword, null))
				.thenThrow(new BusinessException(ErrorCode.CANNOT_USE_KEYWORD));

			System.out.println("=== Keyword 단축 - 이미 존재하는 keyword 예외 ===");
			System.out.println("keyword: " + keyword);

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> handler.execute(command));

			assertEquals(ErrorCode.CANNOT_USE_KEYWORD, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());
		}

		@Test
		@DisplayName("Keyword 단축 실패 - 유효하지 않은 keyword 형식, INVALID_KEYWORD_FORMAT 예외 전파")
		void keyword_단축_유효하지않은형식_예외_전파() {
			// Given
			String originalUrl = "https://www.example.com/invalid";
			String invalidKeyword = "he!lo"; // 특수문자 포함
			ShortenUrlCommand command = new ShortenUrlCommand(null, originalUrl, invalidKeyword);

			when(urlShorteningService.generateShortenedUrlWithKeyword(originalUrl, invalidKeyword, null))
				.thenThrow(new BusinessException(ErrorCode.INVALID_KEYWORD_FORMAT));

			System.out.println("=== Keyword 단축 - 유효하지 않은 형식 예외 ===");
			System.out.println("invalidKeyword: " + invalidKeyword);

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> handler.execute(command));

			assertEquals(ErrorCode.INVALID_KEYWORD_FORMAT, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());
		}
	}

	// -------------------------------------------------------------------------
	// URL 유효성 검증
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("originalUrl 유효성 검증")
	class OriginalUrlValidation {

		@Test
		@DisplayName("URL 검증 실패 - null URL, INVALID_ORIGINAL_URL 예외 전파")
		void url_검증_null_예외_전파() {
			// Given
			ShortenUrlCommand command = new ShortenUrlCommand(null, null, null);

			doThrow(new BusinessException(ErrorCode.INVALID_ORIGINAL_URL))
				.when(urlShorteningService).validateOriginalUrl(null);

			System.out.println("=== originalUrl null 검증 ===");

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> handler.execute(command));

			assertEquals(ErrorCode.INVALID_ORIGINAL_URL, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			verify(urlShorteningService).validateOriginalUrl(null);
			verifyNoInteractions(keyBlockManager);
		}

		@Test
		@DisplayName("URL 검증 실패 - http/https 없음, INVALID_ORIGINAL_URL 예외 전파")
		void url_검증_프로토콜_없음_예외_전파() {
			// Given
			String invalidUrl = "www.example.com";
			ShortenUrlCommand command = new ShortenUrlCommand(null, invalidUrl, null);

			doThrow(new BusinessException(ErrorCode.INVALID_ORIGINAL_URL))
				.when(urlShorteningService).validateOriginalUrl(invalidUrl);

			System.out.println("=== originalUrl 프로토콜 없음 검증 ===");
			System.out.println("invalidUrl: " + invalidUrl);

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> handler.execute(command));

			assertEquals(ErrorCode.INVALID_ORIGINAL_URL, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			verify(urlShorteningService).validateOriginalUrl(invalidUrl);
			verifyNoInteractions(keyBlockManager);
		}

		@Test
		@DisplayName("URL 검증 성공 - validateOriginalUrl이 항상 먼저 호출됨")
		void url_검증_항상_먼저_호출() {
			// Given
			String originalUrl = "https://www.example.com";
			ShortenUrlCommand command = new ShortenUrlCommand(null, originalUrl, null);

			when(keyBlockManager.getNextId()).thenReturn(26001L);
			when(idScrambler.scramble(anyLong())).thenReturn(99999L);
			when(base62Encoder.encode(anyLong())).thenReturn("abc1234");
			when(urlShorteningService.generateShortenedUrl(any(), anyLong(), anyLong(), anyString(), any()))
				.thenReturn(Optional.of("https://lill.ing/abc1234"));

			System.out.println("=== validateOriginalUrl 호출 순서 검증 ===");

			// When
			handler.execute(command);

			// Then
			var inOrder = inOrder(urlShorteningService, keyBlockManager);
			inOrder.verify(urlShorteningService).validateOriginalUrl(originalUrl);
			inOrder.verify(keyBlockManager).getNextId();

			System.out.println("validateOriginalUrl이 getNextId보다 먼저 호출됨 확인");
		}
	}

	// -------------------------------------------------------------------------
	// getSupportedCommandType 검증
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("getSupportedCommandType 검증")
	class SupportedCommandType {

		@Test
		@DisplayName("ShortenUrlCommand 클래스를 반환해야 함")
		void getSupportedCommandType_ShortenUrlCommand_반환() {
			// When
			Class<ShortenUrlCommand> type = handler.getSupportedCommandType();

			// Then
			assertEquals(ShortenUrlCommand.class, type);

			System.out.println("=== getSupportedCommandType 검증 ===");
			System.out.println("반환 타입: " + type.getSimpleName());
		}
	}
}
