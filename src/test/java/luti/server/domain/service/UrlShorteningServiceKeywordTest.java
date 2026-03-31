package luti.server.domain.service;

import static luti.server.exception.ErrorCode.*;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import luti.server.domain.enums.Provider;
import luti.server.domain.model.Member;
import luti.server.domain.model.UrlMapping;
import luti.server.domain.port.AtomicUrlMappingInserter;
import luti.server.domain.port.MemberReader;
import luti.server.domain.util.Base62Encoder;
import luti.server.domain.util.IdScrambler;
import luti.server.domain.util.KeywordSuffixScrambler;
import luti.server.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlShorteningService - generateShortenedUrlWithKeyword 단위 테스트")
class UrlShorteningServiceKeywordTest {

	@Mock
	private MemberReader memberReader;

	@Mock
	private Base62Encoder base62Encoder;

	@Mock
	private IdScrambler idScrambler;

	@Mock
	private AtomicUrlMappingInserter atomicInserter;

	@Mock
	private KeywordSuffixScrambler keywordSuffixScrambler;

	@Mock
	private RedisTemplate<String, Long> counterRedisTemplate;

	@Mock
	private ValueOperations<String, Long> valueOperations;

	private UrlShorteningService urlShorteningService;

	private static final String TEST_DOMAIN = "https://lill.ing";
	private static final String TEST_APP_ID = "test-app";
	private static final String ORIGINAL_URL = "https://example.com/some/long/url";

	@BeforeEach
	void setUp() {
		urlShorteningService = new UrlShorteningService(
			memberReader,
			base62Encoder,
			idScrambler,
			atomicInserter,
			keywordSuffixScrambler,
			counterRedisTemplate
		);

		ReflectionTestUtils.setField(urlShorteningService, "DOMAIN", TEST_DOMAIN);
		ReflectionTestUtils.setField(urlShorteningService, "APP_ID", TEST_APP_ID);
	}

	// -------------------------------------------------------------------------
	// 시나리오 1: keyword 자체로 첫 시도 성공 (7글자 keyword만 해당)
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("7글자 keyword 직접 삽입 성공")
	class KeywordDirectInsertSuccess {

		@Test
		@DisplayName("7글자 keyword 직접 삽입 성공 - 멤버 없음 (비로그인)")
		void keyword_7글자_직접삽입_성공_비로그인() {
			// Given
			String keyword = "abcdefg"; // 7글자

			when(base62Encoder.decode(keyword)).thenReturn(99999L);
			when(idScrambler.descramble(99999L)).thenReturn(12345L);
			when(atomicInserter.tryInsert(any(UrlMapping.class))).thenReturn(true);

			System.out.println("=== 7글자 keyword 직접 삽입 성공 (비로그인) ===");
			System.out.println("keyword: " + keyword + " (길이: " + keyword.length() + ")");

			// When
			String result = urlShorteningService.generateShortenedUrlWithKeyword(
				ORIGINAL_URL, keyword, null);

			// Then
			assertNotNull(result);
			assertEquals(TEST_DOMAIN + "/" + keyword, result);

			System.out.println("생성된 shortUrl: " + result);

			verify(atomicInserter, times(1)).tryInsert(any(UrlMapping.class));
			verifyNoInteractions(counterRedisTemplate);
			verifyNoInteractions(keywordSuffixScrambler);
		}

		@Test
		@DisplayName("7글자 keyword 직접 삽입 성공 - 멤버 있음 (로그인)")
		void keyword_7글자_직접삽입_성공_로그인() {
			// Given
			String keyword = "abcdefg"; // 7글자
			Long memberId = 1L;
			Member member = new Member(Provider.GOOGLE, "google-sub", "test@example.com");

			when(memberReader.findById(memberId)).thenReturn(Optional.of(member));
			when(base62Encoder.decode(keyword)).thenReturn(99999L);
			when(idScrambler.descramble(99999L)).thenReturn(12345L);
			when(atomicInserter.tryInsert(any(UrlMapping.class))).thenReturn(true);

			System.out.println("=== 7글자 keyword 직접 삽입 성공 (로그인) ===");
			System.out.println("keyword: " + keyword);
			System.out.println("memberId: " + memberId);

			// When
			String result = urlShorteningService.generateShortenedUrlWithKeyword(
				ORIGINAL_URL, keyword, memberId);

			// Then
			assertNotNull(result);
			assertEquals(TEST_DOMAIN + "/" + keyword, result);

			System.out.println("생성된 shortUrl: " + result);

			verify(memberReader).findById(memberId);
			verify(atomicInserter, times(1)).tryInsert(any(UrlMapping.class));
		}
	}

	// -------------------------------------------------------------------------
	// 시나리오 2: keyword 길이 < 7 → suffix 루프에서 성공
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("keyword 길이 < 7: suffix 루프에서 성공")
	class KeywordSuffixLoopSuccess {

		@Test
		@DisplayName("suffix 첫 번째 시도 성공 (keyword 2글자)")
		void suffix_첫시도_성공() {
			// Given
			String keyword = "hi"; // 2글자, suffix 공간 있음
			long suffixSpace = 3_844L; // 62^2

			when(idScrambler.descramble(anyLong())).thenReturn(50L);
			when(keywordSuffixScrambler.suffixSpace(keyword)).thenReturn(suffixSpace);
			when(keywordSuffixScrambler.scramble(1L, keyword)).thenReturn(777L); // N=rawCounter=1
			when(base62Encoder.encode(777L)).thenReturn("cX");
			when(base62Encoder.decode(eq("hicX"))).thenReturn(200L);
			when(atomicInserter.tryInsert(any(UrlMapping.class))).thenReturn(true);

			when(counterRedisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.increment("kw:cnt:" + keyword)).thenReturn(1L); // rawCounter=1, N=1

			System.out.println("=== keyword 짧은 경우 suffix 루프 첫 시도 성공 ===");
			System.out.println("keyword: " + keyword);
			System.out.println("suffixSpace: " + suffixSpace);

			// When
			String result = urlShorteningService.generateShortenedUrlWithKeyword(
				ORIGINAL_URL, keyword, null);

			// Then
			assertNotNull(result);
			assertTrue(result.startsWith(TEST_DOMAIN + "/"));

			System.out.println("생성된 shortUrl: " + result);

			verify(atomicInserter, times(1)).tryInsert(any(UrlMapping.class));
			verify(counterRedisTemplate).opsForValue();
			verify(valueOperations).increment("kw:cnt:" + keyword);
		}

		@Test
		@DisplayName("suffix 루프 두 번째 시도에서 성공 (keyword 4글자)")
		void suffix_두번째시도_성공() {
			// Given
			String keyword = "test"; // 4글자
			long suffixSpace = 238_328L; // 62^3

			when(idScrambler.descramble(anyLong())).thenReturn(50L);
			when(keywordSuffixScrambler.suffixSpace(keyword)).thenReturn(suffixSpace);

			// attempt=0: N=rawCounter=1, scramble(1, keyword) -> 111, encode(111) -> "1Z"
			when(keywordSuffixScrambler.scramble(1L, keyword)).thenReturn(111L);
			when(base62Encoder.encode(111L)).thenReturn("1Z");
			when(base62Encoder.decode("test1Z")).thenReturn(500L);

			// attempt=1: N=rawCounter=2, scramble(2, keyword) -> 222, encode(222) -> "3g"
			when(keywordSuffixScrambler.scramble(2L, keyword)).thenReturn(222L);
			when(base62Encoder.encode(222L)).thenReturn("3g");
			when(base62Encoder.decode("test3g")).thenReturn(600L);

			// attempt=0 실패, attempt=1 성공
			when(atomicInserter.tryInsert(any(UrlMapping.class)))
				.thenReturn(false)
				.thenReturn(true);

			when(counterRedisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.increment("kw:cnt:" + keyword))
				.thenReturn(1L) // attempt=0: N=1
				.thenReturn(2L); // attempt=1: N=2

			System.out.println("=== keyword suffix 루프 두 번째 시도 성공 ===");
			System.out.println("keyword: " + keyword);

			// When
			String result = urlShorteningService.generateShortenedUrlWithKeyword(
				ORIGINAL_URL, keyword, null);

			// Then
			assertNotNull(result);
			System.out.println("생성된 shortUrl: " + result);

			verify(atomicInserter, times(2)).tryInsert(any(UrlMapping.class));
			verify(valueOperations, times(2)).increment("kw:cnt:" + keyword);
		}

		@Test
		@DisplayName("suffix 첫 번째 시도 성공 - 멤버 없음 (1글자 keyword)")
		void suffix_1글자keyword_첫시도_성공() {
			// Given
			String keyword = "a"; // 1글자
			long suffixSpace = 14_776_336L; // 62^4 (suffix 최대 6자)

			when(idScrambler.descramble(anyLong())).thenReturn(5L);
			when(keywordSuffixScrambler.suffixSpace(keyword)).thenReturn(suffixSpace);
			when(keywordSuffixScrambler.scramble(1L, keyword)).thenReturn(123L); // N=rawCounter=1
			when(base62Encoder.encode(123L)).thenReturn("B1");
			when(base62Encoder.decode("aB1")).thenReturn(300L);
			when(atomicInserter.tryInsert(any(UrlMapping.class))).thenReturn(true);

			when(counterRedisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.increment("kw:cnt:" + keyword)).thenReturn(1L);

			System.out.println("=== 1글자 keyword suffix 첫 시도 성공 ===");
			System.out.println("keyword: " + keyword);

			// When
			String result = urlShorteningService.generateShortenedUrlWithKeyword(
				ORIGINAL_URL, keyword, null);

			// Then
			assertNotNull(result);
			System.out.println("생성된 shortUrl: " + result);

			verify(atomicInserter, times(1)).tryInsert(any(UrlMapping.class));
		}
	}

	// -------------------------------------------------------------------------
	// 시나리오 3: 7글자 keyword가 이미 존재 → CANNOT_USE_KEYWORD 예외
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("7글자 keyword 충돌 시 즉시 실패")
	class SevenCharKeywordCollision {

		@Test
		@DisplayName("7글자 keyword 충돌 - CANNOT_USE_KEYWORD 예외 발생")
		void keyword_7글자_충돌_예외() {
			// Given
			String keyword = "abcdefg"; // 7글자

			when(base62Encoder.decode(keyword)).thenReturn(99999L);
			when(idScrambler.descramble(99999L)).thenReturn(12345L);
			// 첫 tryInsert 실패 (이미 존재)
			when(atomicInserter.tryInsert(any(UrlMapping.class))).thenReturn(false);

			System.out.println("=== 7글자 keyword 충돌 - 즉시 CANNOT_USE_KEYWORD 예외 ===");
			System.out.println("keyword: " + keyword + " (길이: " + keyword.length() + ")");

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> urlShorteningService.generateShortenedUrlWithKeyword(
					ORIGINAL_URL, keyword, null));

			assertEquals(CANNOT_USE_KEYWORD, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			// suffix 로직으로 진입하지 않음
			verifyNoInteractions(counterRedisTemplate);
			verifyNoInteractions(keywordSuffixScrambler);
		}

		@Test
		@DisplayName("7글자 keyword 충돌 - suffix 루프가 호출되지 않음을 확인")
		void keyword_7글자_충돌_suffix루프_미진입() {
			// Given
			String keyword = "ZZZZZZZ"; // 7글자 (대문자)

			when(base62Encoder.decode(keyword)).thenReturn(77777L);
			when(idScrambler.descramble(77777L)).thenReturn(33333L);
			when(atomicInserter.tryInsert(any(UrlMapping.class))).thenReturn(false);

			System.out.println("=== 7글자 keyword(대문자) 충돌 검증 ===");
			System.out.println("keyword: " + keyword);

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> urlShorteningService.generateShortenedUrlWithKeyword(
					ORIGINAL_URL, keyword, null));

			assertEquals(CANNOT_USE_KEYWORD, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			verify(atomicInserter, times(1)).tryInsert(any(UrlMapping.class));
			verifyNoInteractions(counterRedisTemplate);
			verifyNoInteractions(keywordSuffixScrambler);
		}
	}

	// -------------------------------------------------------------------------
	// 시나리오 4: N >= suffixSpace → CANNOT_USE_KEYWORD 예외
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("suffix 공간 초과 시 실패")
	class SuffixSpaceExhausted {

		@Test
		@DisplayName("N >= suffixSpace - CANNOT_USE_KEYWORD 예외 발생")
		void N이_suffixSpace_이상일때_예외() {
			// Given
			String keyword = "abc"; // 3글자
			long suffixSpace = 14_776_336L; // 62^4

			when(keywordSuffixScrambler.suffixSpace(keyword)).thenReturn(suffixSpace);

			when(counterRedisTemplate.opsForValue()).thenReturn(valueOperations);
			// rawCounter = suffixSpace + 1 → N = suffixSpace + 1 >= suffixSpace → 초과
			when(valueOperations.increment("kw:cnt:" + keyword))
				.thenReturn(suffixSpace + 1);

			System.out.println("=== N >= suffixSpace 시 CANNOT_USE_KEYWORD 예외 ===");
			System.out.println("keyword: " + keyword);
			System.out.println("suffixSpace: " + suffixSpace);
			System.out.println("rawCounter: " + (suffixSpace + 1) + " -> N=" + (suffixSpace + 1));

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> urlShorteningService.generateShortenedUrlWithKeyword(
					ORIGINAL_URL, keyword, null));

			assertEquals(CANNOT_USE_KEYWORD, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			verify(valueOperations, times(1)).increment("kw:cnt:" + keyword);
			// N >= suffixSpace이므로 scramble은 호출되지 않음
			verify(keywordSuffixScrambler, never()).scramble(anyLong(), anyString());
			// tryInsert도 호출되지 않음
			verifyNoInteractions(atomicInserter);
		}

		@Test
		@DisplayName("N == suffixSpace (경계값) - CANNOT_USE_KEYWORD 예외 발생")
		void N이_suffixSpace와_같을때_경계값_예외() {
			// Given
			String keyword = "go"; // 2글자
			long suffixSpace = 3_844L; // 62^2

			when(keywordSuffixScrambler.suffixSpace(keyword)).thenReturn(suffixSpace);

			when(counterRedisTemplate.opsForValue()).thenReturn(valueOperations);
			// rawCounter = suffixSpace → N = suffixSpace (경계값, 정확히 초과)
			when(valueOperations.increment("kw:cnt:" + keyword))
				.thenReturn(suffixSpace);

			System.out.println("=== N == suffixSpace 경계값 테스트 ===");
			System.out.println("keyword: " + keyword);
			System.out.println("suffixSpace: " + suffixSpace);
			System.out.println("N (경계값): " + suffixSpace);

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> urlShorteningService.generateShortenedUrlWithKeyword(
					ORIGINAL_URL, keyword, null));

			assertEquals(CANNOT_USE_KEYWORD, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());
		}
	}

	// -------------------------------------------------------------------------
	// 시나리오 5: MAX_COUNTER_RETRY 초과 → CANNOT_USE_KEYWORD 예외
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("MAX_COUNTER_RETRY 초과 시 실패")
	class MaxRetryExceeded {

		@Test
		@DisplayName("MAX_COUNTER_RETRY(5회) 모두 충돌 - CANNOT_USE_KEYWORD 예외 발생")
		void 최대재시도_초과_예외() {
			// Given
			String keyword = "url"; // 3글자
			long suffixSpace = 14_776_336L; // 62^4

			when(base62Encoder.decode(anyString())).thenReturn(100L);
			when(idScrambler.descramble(anyLong())).thenReturn(50L);
			when(keywordSuffixScrambler.suffixSpace(keyword)).thenReturn(suffixSpace);

			// attempt 0~4 각각 scramble 반환값 설정
			when(keywordSuffixScrambler.scramble(1L, keyword)).thenReturn(10L);
			when(keywordSuffixScrambler.scramble(2L, keyword)).thenReturn(20L);
			when(keywordSuffixScrambler.scramble(3L, keyword)).thenReturn(30L);
			when(keywordSuffixScrambler.scramble(4L, keyword)).thenReturn(40L);
			when(keywordSuffixScrambler.scramble(5L, keyword)).thenReturn(50L);

			when(base62Encoder.encode(10L)).thenReturn("A");
			when(base62Encoder.encode(20L)).thenReturn("B");
			when(base62Encoder.encode(30L)).thenReturn("C");
			when(base62Encoder.encode(40L)).thenReturn("D");
			when(base62Encoder.encode(50L)).thenReturn("E");

			// 5번 retry 모두 삽입 실패 (총 5번)
			when(atomicInserter.tryInsert(any(UrlMapping.class))).thenReturn(false);

			when(counterRedisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.increment("kw:cnt:" + keyword))
				.thenReturn(1L)  // attempt 0: N=1
				.thenReturn(2L)  // attempt 1: N=2
				.thenReturn(3L)  // attempt 2: N=3
				.thenReturn(4L)  // attempt 3: N=4
				.thenReturn(5L); // attempt 4: N=5

			System.out.println("=== MAX_COUNTER_RETRY(5회) 모두 충돌 ===");
			System.out.println("keyword: " + keyword);

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> urlShorteningService.generateShortenedUrlWithKeyword(
					ORIGINAL_URL, keyword, null));

			assertEquals(CANNOT_USE_KEYWORD, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			// 5번 retry = 총 5번 tryInsert 호출
			verify(atomicInserter, times(5)).tryInsert(any(UrlMapping.class));
			// 5번 counter increment
			verify(valueOperations, times(5)).increment("kw:cnt:" + keyword);
		}

		@Test
		@DisplayName("MAX_COUNTER_RETRY - 4번 실패 후 마지막(5번째) 시도 성공")
		void 최대재시도_직전_성공() {
			// Given
			String keyword = "ok"; // 2글자
			long suffixSpace = 3_844L; // 62^2

			when(base62Encoder.decode(anyString())).thenReturn(100L);
			when(idScrambler.descramble(anyLong())).thenReturn(50L);
			when(keywordSuffixScrambler.suffixSpace(keyword)).thenReturn(suffixSpace);

			when(keywordSuffixScrambler.scramble(1L, keyword)).thenReturn(1L);
			when(keywordSuffixScrambler.scramble(2L, keyword)).thenReturn(2L);
			when(keywordSuffixScrambler.scramble(3L, keyword)).thenReturn(3L);
			when(keywordSuffixScrambler.scramble(4L, keyword)).thenReturn(4L);
			when(keywordSuffixScrambler.scramble(5L, keyword)).thenReturn(5L);

			when(base62Encoder.encode(1L)).thenReturn("X");
			when(base62Encoder.encode(2L)).thenReturn("Y");
			when(base62Encoder.encode(3L)).thenReturn("Z");
			when(base62Encoder.encode(4L)).thenReturn("W");
			when(base62Encoder.encode(5L)).thenReturn("V");

			// attempt 0~3 실패 + attempt 4 성공
			when(atomicInserter.tryInsert(any(UrlMapping.class)))
				.thenReturn(false)
				.thenReturn(false)
				.thenReturn(false)
				.thenReturn(false)
				.thenReturn(true); // attempt 4: 성공

			when(counterRedisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.increment("kw:cnt:" + keyword))
				.thenReturn(1L)
				.thenReturn(2L)
				.thenReturn(3L)
				.thenReturn(4L)
				.thenReturn(5L);

			System.out.println("=== 최대 재시도 직전 (5번째) 성공 ===");
			System.out.println("keyword: " + keyword);

			// When
			String result = urlShorteningService.generateShortenedUrlWithKeyword(
				ORIGINAL_URL, keyword, null);

			// Then
			assertNotNull(result);
			System.out.println("생성된 shortUrl: " + result);

			verify(atomicInserter, times(5)).tryInsert(any(UrlMapping.class));
			verify(valueOperations, times(5)).increment("kw:cnt:" + keyword);
		}
	}

	// -------------------------------------------------------------------------
	// 시나리오 6: 유효하지 않은 keyword 형식 → INVALID_KEYWORD_FORMAT 예외
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("유효하지 않은 keyword 형식 검증")
	class InvalidKeywordFormat {

		@Test
		@DisplayName("빈 문자열 keyword - INVALID_KEYWORD_FORMAT 예외 발생")
		void keyword_빈문자열_예외() {
			// Given
			String keyword = "";

			System.out.println("=== 빈 문자열 keyword 검증 ===");

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> urlShorteningService.generateShortenedUrlWithKeyword(
					ORIGINAL_URL, keyword, null));

			assertEquals(INVALID_KEYWORD_FORMAT, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			verifyNoInteractions(atomicInserter);
			verifyNoInteractions(counterRedisTemplate);
		}

		@Test
		@DisplayName("8글자 이상 keyword - INVALID_KEYWORD_FORMAT 예외 발생")
		void keyword_8글자이상_예외() {
			// Given
			String keyword = "toolong1"; // 8글자

			System.out.println("=== 8글자 이상 keyword 검증 ===");
			System.out.println("keyword: " + keyword + " (길이: " + keyword.length() + ")");

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> urlShorteningService.generateShortenedUrlWithKeyword(
					ORIGINAL_URL, keyword, null));

			assertEquals(INVALID_KEYWORD_FORMAT, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			verifyNoInteractions(atomicInserter);
		}

		@Test
		@DisplayName("특수문자 포함 keyword - INVALID_KEYWORD_FORMAT 예외 발생")
		void keyword_특수문자포함_예외() {
			// Given
			String keyword = "ab-cd";

			System.out.println("=== 특수문자 포함 keyword 검증 ===");
			System.out.println("keyword: " + keyword);

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> urlShorteningService.generateShortenedUrlWithKeyword(
					ORIGINAL_URL, keyword, null));

			assertEquals(INVALID_KEYWORD_FORMAT, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			verifyNoInteractions(atomicInserter);
		}

		@Test
		@DisplayName("공백 포함 keyword - INVALID_KEYWORD_FORMAT 예외 발생")
		void keyword_공백포함_예외() {
			// Given
			String keyword = "ab cd";

			System.out.println("=== 공백 포함 keyword 검증 ===");
			System.out.println("keyword: \"" + keyword + "\"");

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> urlShorteningService.generateShortenedUrlWithKeyword(
					ORIGINAL_URL, keyword, null));

			assertEquals(INVALID_KEYWORD_FORMAT, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			verifyNoInteractions(atomicInserter);
		}

		@Test
		@DisplayName("한글 포함 keyword - INVALID_KEYWORD_FORMAT 예외 발생")
		void keyword_한글포함_예외() {
			// Given
			String keyword = "abc한글";

			System.out.println("=== 한글 포함 keyword 검증 ===");
			System.out.println("keyword: " + keyword);

			// When & Then
			BusinessException exception = assertThrows(BusinessException.class,
				() -> urlShorteningService.generateShortenedUrlWithKeyword(
					ORIGINAL_URL, keyword, null));

			assertEquals(INVALID_KEYWORD_FORMAT, exception.getErrorCode());

			System.out.println("예외 발생: " + exception.getMessage());

			verifyNoInteractions(atomicInserter);
		}

		@Test
		@DisplayName("유효한 keyword - 소문자만 (1글자) suffix 루프 진입 성공")
		void keyword_소문자_1글자_성공() {
			// Given
			String keyword = "a";
			long suffixSpace = 14_776_336L;

			when(idScrambler.descramble(anyLong())).thenReturn(5L);
			when(keywordSuffixScrambler.suffixSpace(keyword)).thenReturn(suffixSpace);
			when(keywordSuffixScrambler.scramble(1L, keyword)).thenReturn(100L); // N=rawCounter=1
			when(base62Encoder.encode(100L)).thenReturn("Bc");
			when(base62Encoder.decode("aBc")).thenReturn(20L);
			when(atomicInserter.tryInsert(any(UrlMapping.class))).thenReturn(true);
			when(counterRedisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.increment("kw:cnt:" + keyword)).thenReturn(1L);

			System.out.println("=== 유효한 keyword - 소문자 1글자 ===");
			System.out.println("keyword: " + keyword);

			// When
			String result = urlShorteningService.generateShortenedUrlWithKeyword(
				ORIGINAL_URL, keyword, null);

			// Then
			assertNotNull(result);

			System.out.println("생성된 shortUrl: " + result);
		}

		@Test
		@DisplayName("유효한 keyword - 대소문자 숫자 혼합 (7글자) 직접 삽입 성공")
		void keyword_대소문자숫자혼합_7글자_성공() {
			// Given
			String keyword = "aB3cD4e"; // 7글자, 대소문자 숫자 혼합 - 유효

			when(base62Encoder.decode(keyword)).thenReturn(99999L);
			when(idScrambler.descramble(99999L)).thenReturn(12345L);
			when(atomicInserter.tryInsert(any(UrlMapping.class))).thenReturn(true);

			System.out.println("=== 유효한 keyword - 대소문자 숫자 혼합 7글자 ===");
			System.out.println("keyword: " + keyword);

			// When
			String result = urlShorteningService.generateShortenedUrlWithKeyword(
				ORIGINAL_URL, keyword, null);

			// Then
			assertNotNull(result);
			assertEquals(TEST_DOMAIN + "/" + keyword, result);

			System.out.println("생성된 shortUrl: " + result);
		}
	}

	// -------------------------------------------------------------------------
	// 부가 시나리오: shortUrl 형식 및 내용 검증
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("생성된 shortUrl 형식 검증")
	class ShortUrlFormatVerification {

		@Test
		@DisplayName("7글자 keyword 직접 삽입 성공 - shortUrl은 DOMAIN/keyword 형식")
		void shortUrl_형식_검증() {
			// Given
			String keyword = "abcdefg"; // 7글자

			when(base62Encoder.decode(keyword)).thenReturn(12345L);
			when(idScrambler.descramble(12345L)).thenReturn(999L);
			when(atomicInserter.tryInsert(any(UrlMapping.class))).thenReturn(true);

			System.out.println("=== shortUrl 형식 검증 ===");
			System.out.println("DOMAIN: " + TEST_DOMAIN);
			System.out.println("keyword: " + keyword);
			System.out.println("예상 shortUrl: " + TEST_DOMAIN + "/" + keyword);

			// When
			String result = urlShorteningService.generateShortenedUrlWithKeyword(
				ORIGINAL_URL, keyword, null);

			// Then
			assertEquals(TEST_DOMAIN + "/" + keyword, result);

			System.out.println("실제 shortUrl: " + result);
		}

		@Test
		@DisplayName("suffix 포함 성공 - shortUrl은 DOMAIN/keyword+suffix 형식")
		void suffix포함_shortUrl_형식_검증() {
			// Given
			String keyword = "my"; // 2글자
			String suffix = "Ab";
			long suffixSpace = 3_844L;

			when(idScrambler.descramble(anyLong())).thenReturn(50L);
			when(keywordSuffixScrambler.suffixSpace(keyword)).thenReturn(suffixSpace);
			when(keywordSuffixScrambler.scramble(1L, keyword)).thenReturn(999L); // N=rawCounter=1
			when(base62Encoder.encode(999L)).thenReturn(suffix);
			when(base62Encoder.decode(keyword + suffix)).thenReturn(200L);
			when(atomicInserter.tryInsert(any(UrlMapping.class))).thenReturn(true);

			when(counterRedisTemplate.opsForValue()).thenReturn(valueOperations);
			when(valueOperations.increment("kw:cnt:" + keyword)).thenReturn(1L);

			System.out.println("=== suffix 포함 shortUrl 형식 검증 ===");
			System.out.println("keyword: " + keyword + ", suffix: " + suffix);
			System.out.println("예상 shortUrl: " + TEST_DOMAIN + "/" + keyword + suffix);

			// When
			String result = urlShorteningService.generateShortenedUrlWithKeyword(
				ORIGINAL_URL, keyword, null);

			// Then
			assertNotNull(result);
			assertEquals(TEST_DOMAIN + "/" + keyword + suffix, result);

			System.out.println("실제 shortUrl: " + result);
		}
	}

	// -------------------------------------------------------------------------
	// 부가 시나리오: 멤버 조회 관련
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("멤버 조회 동작 검증")
	class MemberResolution {

		@Test
		@DisplayName("memberId가 null이면 memberReader를 호출하지 않음")
		void memberId_null_memberReader_미호출() {
			// Given
			String keyword = "abcdefg"; // 7글자 - 직접 삽입 경로

			when(base62Encoder.decode(keyword)).thenReturn(300L);
			when(idScrambler.descramble(300L)).thenReturn(150L);
			when(atomicInserter.tryInsert(any(UrlMapping.class))).thenReturn(true);

			System.out.println("=== memberId=null 시 memberReader 미호출 검증 ===");

			// When
			urlShorteningService.generateShortenedUrlWithKeyword(ORIGINAL_URL, keyword, null);

			// Then
			verifyNoInteractions(memberReader);
		}

		@Test
		@DisplayName("memberId가 있으면 memberReader.findById를 호출하여 멤버 조회")
		void memberId_있음_memberReader_호출() {
			// Given
			String keyword = "abcdefg"; // 7글자 - 직접 삽입 경로
			Long memberId = 42L;
			Member member = new Member(Provider.GOOGLE, "google-42", "user42@example.com");

			when(memberReader.findById(memberId)).thenReturn(Optional.of(member));
			when(base62Encoder.decode(keyword)).thenReturn(300L);
			when(idScrambler.descramble(300L)).thenReturn(150L);
			when(atomicInserter.tryInsert(any(UrlMapping.class))).thenReturn(true);

			System.out.println("=== memberId 존재 시 memberReader 호출 검증 ===");
			System.out.println("memberId: " + memberId);

			// When
			urlShorteningService.generateShortenedUrlWithKeyword(ORIGINAL_URL, keyword, memberId);

			// Then
			verify(memberReader).findById(memberId);
		}

		@Test
		@DisplayName("memberId가 있지만 존재하지 않는 멤버 - member=null로 처리 (익명 처리)")
		void memberId_존재하지않음_null멤버_처리() {
			// Given
			String keyword = "abcdefg"; // 7글자 - 직접 삽입 경로
			Long memberId = 999L;

			// memberReader가 empty Optional 반환
			when(memberReader.findById(memberId)).thenReturn(Optional.empty());
			when(base62Encoder.decode(keyword)).thenReturn(300L);
			when(idScrambler.descramble(300L)).thenReturn(150L);
			when(atomicInserter.tryInsert(any(UrlMapping.class))).thenReturn(true);

			System.out.println("=== 존재하지 않는 memberId → null 멤버로 처리 ===");
			System.out.println("memberId: " + memberId);

			// When
			String result = urlShorteningService.generateShortenedUrlWithKeyword(
				ORIGINAL_URL, keyword, memberId);

			// Then - 예외 없이 처리됨 (member=null로 익명 처리)
			assertNotNull(result);

			System.out.println("생성된 shortUrl: " + result);

			verify(memberReader).findById(memberId);
			verify(atomicInserter).tryInsert(any(UrlMapping.class));
		}
	}
}
