package luti.server.web.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import luti.server.application.bus.CommandBus;
import luti.server.application.command.ShortenUrlCommand;
import luti.server.application.result.ShortenUrlResult;
import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;
import luti.server.web.resolver.CommandArgumentResolver;
import luti.server.exception.GlobalExceptionHandler;

/**
 * UrlShorteningController 슬라이스 테스트.
 *
 * Spring Security 자동설정과 OAuth2 자동설정을 모두 제외하고,
 * addFilters = false 로 Security Filter Chain을 비활성화한다.
 * 이를 통해 인증/인가 로직 없이 컨트롤러 레이어만 순수하게 테스트한다.
 *
 * CommandArgumentResolver 는 @WebMvcTest 스캔 대상이 아니므로
 * @Import 로 명시적으로 등록한다.
 *
 * @EnableJpaAuditing 이 메인 클래스에 선언되어 있어
 * JpaMetamodelMappingContext 를 @MockitoBean 으로 등록해야 한다.
 */
@WebMvcTest(
	controllers = UrlShorteningController.class,
	excludeAutoConfiguration = {
		org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
		org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
		org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
		org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
		org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
	}
)
@AutoConfigureMockMvc(addFilters = false)
@Import({CommandArgumentResolver.class, GlobalExceptionHandler.class})
@DisplayName("UrlShorteningController - MockMvc 슬라이스 테스트")
class UrlShorteningControllerTest {

	@Autowired
	private MockMvc mockMvc;

	// @EnableJpaAuditing 이 메인 클래스에 선언되어 @WebMvcTest 컨텍스트에서
	// JpaMetamodelMappingContext 빈을 찾지 못하는 문제 방지
	@MockitoBean
	private JpaMetamodelMappingContext jpaMetamodelMappingContext;

	@MockitoBean
	private CommandBus commandBus;

	@BeforeEach
	void setUp() {
		reset(commandBus);
	}

	// -------------------------------------------------------------------------
	// POST /api/v1/url/shorten - 성공 케이스
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("POST /api/v1/url/shorten - 성공")
	class ShortenSuccess {

		@Test
		@DisplayName("유효한 URL - 200 OK + shortUrl JSON 반환")
		void 유효한URL_200OK() throws Exception {
			// Given
			String originalUrl = "https://www.example.com";
			String shortUrl = "https://lill.ing/abc1234";
			String requestBody = "{\"originalUrl\":\"" + originalUrl + "\"}";

			when(commandBus.execute(any(ShortenUrlCommand.class)))
				.thenReturn(ShortenUrlResult.of(shortUrl));

			System.out.println("=== URL 단축 성공 ===");
			System.out.println("originalUrl: " + originalUrl);

			// When & Then
			mockMvc.perform(post("/api/v1/url/shorten")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shortUrl").exists())
				.andExpect(jsonPath("$.shortUrl").value(shortUrl));

			System.out.println("shortUrl: " + shortUrl);

			verify(commandBus).execute(any(ShortenUrlCommand.class));
		}

		@Test
		@DisplayName("keyword 포함 - 유효한 keyword + URL, 200 OK + keyword 기반 shortUrl 반환")
		void keyword_포함_유효한URL_200OK() throws Exception {
			// Given
			String originalUrl = "https://www.naver.com";
			String keyword = "mylink";
			String shortUrl = "https://lill.ing/" + keyword;
			String requestBody = "{\"originalUrl\":\"" + originalUrl + "\",\"keyword\":\"" + keyword + "\"}";

			when(commandBus.execute(any(ShortenUrlCommand.class)))
				.thenReturn(ShortenUrlResult.of(shortUrl));

			System.out.println("=== keyword 기반 URL 단축 성공 ===");
			System.out.println("originalUrl: " + originalUrl);
			System.out.println("keyword: " + keyword);

			// When & Then
			mockMvc.perform(post("/api/v1/url/shorten")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shortUrl").exists())
				.andExpect(jsonPath("$.shortUrl").value(shortUrl));

			System.out.println("shortUrl: " + shortUrl);
		}

		@Test
		@DisplayName("응답 JSON 구조 검증 - shortUrl 필드가 루트 레벨에 문자열로 존재해야 함")
		void 응답JSON_구조_검증() throws Exception {
			// Given
			String originalUrl = "https://www.github.com";
			String shortUrl = "https://lill.ing/test123";
			String requestBody = "{\"originalUrl\":\"" + originalUrl + "\"}";

			when(commandBus.execute(any(ShortenUrlCommand.class)))
				.thenReturn(ShortenUrlResult.of(shortUrl));

			System.out.println("=== 응답 JSON 구조 검증 ===");

			// When & Then
			mockMvc.perform(post("/api/v1/url/shorten")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shortUrl").isString())
				.andExpect(jsonPath("$.shortUrl").value(shortUrl));

			System.out.println("응답 JSON 구조 정상 확인");
		}

		@Test
		@DisplayName("keyword 필드 없이 요청 - keyword=null 처리, 200 OK")
		void keyword_필드없이_요청_200OK() throws Exception {
			// Given: keyword 필드 자체를 포함하지 않음
			String originalUrl = "https://www.example.com/no-keyword";
			String shortUrl = "https://lill.ing/nokey01";
			String requestBody = "{\"originalUrl\":\"" + originalUrl + "\"}";

			when(commandBus.execute(any(ShortenUrlCommand.class)))
				.thenReturn(ShortenUrlResult.of(shortUrl));

			System.out.println("=== keyword 필드 없이 요청 ===");
			System.out.println("requestBody: " + requestBody);

			// When & Then
			mockMvc.perform(post("/api/v1/url/shorten")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shortUrl").value(shortUrl));

			System.out.println("shortUrl: " + shortUrl);
		}

		@Test
		@DisplayName("CommandBus가 반환한 shortUrl이 그대로 응답에 포함됨")
		void CommandBus_반환값_응답에_포함() throws Exception {
			// Given
			String expectedShortUrl = "https://lill.ing/XYZ9876";
			String requestBody = "{\"originalUrl\":\"https://www.example.com\"}";

			when(commandBus.execute(any(ShortenUrlCommand.class)))
				.thenReturn(ShortenUrlResult.of(expectedShortUrl));

			System.out.println("=== CommandBus 반환값 응답 포함 검증 ===");
			System.out.println("expectedShortUrl: " + expectedShortUrl);

			// When & Then
			mockMvc.perform(post("/api/v1/url/shorten")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shortUrl").value(expectedShortUrl));

			System.out.println("응답 shortUrl 일치 확인");
		}
	}

	// -------------------------------------------------------------------------
	// POST /api/v1/url/shorten - CommandBus 예외 전파
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("POST /api/v1/url/shorten - 비즈니스 예외 전파")
	class ShortenBusinessException {

		@Test
		@DisplayName("INVALID_ORIGINAL_URL - GlobalExceptionHandler가 400 + 에러코드 3210 반환")
		void INVALID_ORIGINAL_URL_400_반환() throws Exception {
			// Given
			String invalidUrl = "not-a-valid-url";
			String requestBody = "{\"originalUrl\":\"" + invalidUrl + "\"}";

			when(commandBus.execute(any(ShortenUrlCommand.class)))
				.thenThrow(new BusinessException(ErrorCode.INVALID_ORIGINAL_URL));

			System.out.println("=== INVALID_ORIGINAL_URL 에러 응답 검증 ===");
			System.out.println("invalidUrl: " + invalidUrl);
			System.out.println("기대 상태: 400, 기대 코드: 3210");

			// When & Then
			// GlobalExceptionHandler 가 BusinessException 을 처리하여 400 + 에러코드 반환
			mockMvc.perform(post("/api/v1/url/shorten")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("3210"));

			System.out.println("400 Bad Request + 코드 3210 확인");

			verify(commandBus).execute(any(ShortenUrlCommand.class));
		}

		@Test
		@DisplayName("CANNOT_USE_KEYWORD - GlobalExceptionHandler가 400 + 에러코드 3208 반환")
		void CANNOT_USE_KEYWORD_400_반환() throws Exception {
			// Given
			String originalUrl = "https://www.example.com";
			String keyword = "abcdefg";
			String requestBody = "{\"originalUrl\":\"" + originalUrl + "\",\"keyword\":\"" + keyword + "\"}";

			when(commandBus.execute(any(ShortenUrlCommand.class)))
				.thenThrow(new BusinessException(ErrorCode.CANNOT_USE_KEYWORD));

			System.out.println("=== CANNOT_USE_KEYWORD 에러 응답 검증 ===");
			System.out.println("keyword: " + keyword);
			System.out.println("기대 상태: 400, 기대 코드: 3208");

			// When & Then
			mockMvc.perform(post("/api/v1/url/shorten")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("3208"));

			System.out.println("400 Bad Request + 코드 3208 확인");

			verify(commandBus).execute(any(ShortenUrlCommand.class));
		}

		@Test
		@DisplayName("AUTO_SHORTEN_FAILED - GlobalExceptionHandler가 500 + 에러코드 3209 반환")
		void AUTO_SHORTEN_FAILED_500_반환() throws Exception {
			// Given
			String originalUrl = "https://www.example.com";
			String requestBody = "{\"originalUrl\":\"" + originalUrl + "\"}";

			when(commandBus.execute(any(ShortenUrlCommand.class)))
				.thenThrow(new BusinessException(ErrorCode.AUTO_SHORTEN_FAILED));

			System.out.println("=== AUTO_SHORTEN_FAILED 에러 응답 검증 ===");
			System.out.println("기대 상태: 500, 기대 코드: 3209");

			// When & Then
			mockMvc.perform(post("/api/v1/url/shorten")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("3209"));

			System.out.println("500 Internal Server Error + 코드 3209 확인");

			verify(commandBus).execute(any(ShortenUrlCommand.class));
		}
	}

	// -------------------------------------------------------------------------
	// CommandBus 호출 검증
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("CommandBus 호출 검증")
	class CommandBusInteraction {

		@Test
		@DisplayName("단일 요청 - CommandBus.execute가 정확히 1번 호출됨")
		void 단일요청_CommandBus_1번_호출() throws Exception {
			// Given
			String requestBody = "{\"originalUrl\":\"https://www.example.com\"}";

			when(commandBus.execute(any(ShortenUrlCommand.class)))
				.thenReturn(ShortenUrlResult.of("https://lill.ing/abc1234"));

			System.out.println("=== CommandBus.execute 호출 횟수 검증 ===");

			// When
			mockMvc.perform(post("/api/v1/url/shorten")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

			// Then
			verify(commandBus, times(1)).execute(any(ShortenUrlCommand.class));

			System.out.println("CommandBus.execute 1번 호출 확인");
		}

		@Test
		@DisplayName("3번 요청 - CommandBus.execute가 3번 호출됨")
		void 세번요청_CommandBus_3번_호출() throws Exception {
			// Given
			String requestBody = "{\"originalUrl\":\"https://www.example.com\"}";

			when(commandBus.execute(any(ShortenUrlCommand.class)))
				.thenReturn(ShortenUrlResult.of("https://lill.ing/abc1234"));

			System.out.println("=== CommandBus.execute 3번 호출 검증 ===");

			// When
			for (int i = 0; i < 3; i++) {
				mockMvc.perform(post("/api/v1/url/shorten")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody));
			}

			// Then
			verify(commandBus, times(3)).execute(any(ShortenUrlCommand.class));

			System.out.println("CommandBus.execute 3번 호출 확인");
		}

		@Test
		@DisplayName("GET 메서드 요청 - GlobalExceptionHandler가 400 반환, CommandBus 호출 없음")
		void GET_메서드_400() throws Exception {
			// Given: /api/v1/url/shorten 은 POST만 허용
			// GlobalExceptionHandler 가 HttpRequestMethodNotSupportedException 을 400으로 처리함

			System.out.println("=== GET 메서드로 잘못된 요청 ===");

			// When & Then
			mockMvc.perform(get("/api/v1/url/shorten")
					.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());

			System.out.println("400 Bad Request 확인 (GlobalExceptionHandler 처리)");

			verifyNoInteractions(commandBus);
		}
	}
}
