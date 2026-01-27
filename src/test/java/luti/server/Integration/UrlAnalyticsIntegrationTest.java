package luti.server.Integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import luti.server.domain.enums.Provider;
import luti.server.domain.model.ClickCountHistory;
import luti.server.domain.model.Member;
import luti.server.domain.model.UrlMapping;
import luti.server.infrastructure.client.kgs.KeyBlock;
import luti.server.infrastructure.client.kgs.KgsClient;
import luti.server.infrastructure.persistence.ClickCountHistoryRepository;
import luti.server.infrastructure.persistence.MemberRepository;
import luti.server.infrastructure.persistence.UrlMappingRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UrlAnalyticsIntegrationTest {

	@Container
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
		.withDatabaseName("testdb")
		.withUsername("test")
		.withPassword("test");

	@Container
	static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
		.withExposedPorts(6379);

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", redis::getFirstMappedPort);

		registry.add("JWT_SECRET_KEY", () -> "test-secret-key-for-jwt-signing-at-least-32-characters-long");
		registry.add("JWT_ACCESS_TTL_SECONDS", () -> "3600");
		registry.add("JWT_ISSUER", () -> "https://test.lill.ing");
		registry.add("JWT_AUDIENCE", () -> "test.lill.ing");

		registry.add("APP_ID", () -> "test-app");
		registry.add("DOMAIN", () -> "lill.ing");
		registry.add("SCRAMBLING_CONST_XOR1", () -> "13");
		registry.add("SCRAMBLING_CONST_XOR2", () -> "7");
		registry.add("SCRAMBLING_CONST_XOR3", () -> "17");
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private KgsClient kgsClient;

	@Autowired
	private UrlMappingRepository urlMappingRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private ClickCountHistoryRepository clickCountHistoryRepository;

	@BeforeEach
	void setUp() {
		clickCountHistoryRepository.deleteAll();
		urlMappingRepository.deleteAll();
		memberRepository.deleteAll();

		when(kgsClient.fetchNextBlock())
			.thenReturn(new KeyBlock(26001, 27000))
			.thenReturn(new KeyBlock(27001, 28000));
	}

	@Test
	@DisplayName("URL 통계 조회 - 정상 동작 (데이터 없음)")
	void getUrlAnalytics_정상동작_데이터없음() throws Exception {
		// Given: Member와 URL 생성
		Member member = new Member(Provider.GOOGLE, "google-analytics", "analytics@example.com");
		Member savedMember = memberRepository.save(member);

		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		Long urlId = saved.getId();

		System.out.println("=== URL 통계 조회 테스트 (데이터 없음) ===");
		System.out.println("URL ID: " + urlId);
		System.out.println("Member ID: " + savedMember.getId());

		// When & Then
		mockMvc.perform(get("/api/v1/my-urls/" + urlId + "/analytics")
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.hourly").exists())
			.andExpect(jsonPath("$.hourly.range").value("24h"))
			.andExpect(jsonPath("$.hourly.data").isArray())
			.andExpect(jsonPath("$.hourly.data.length()").value(24))
			.andExpect(jsonPath("$.daily").exists())
			.andExpect(jsonPath("$.daily.range").value("30d"))
			.andExpect(jsonPath("$.daily.data").isArray())
			.andExpect(jsonPath("$.daily.data.length()").value(30))
			.andExpect(jsonPath("$.weekly").exists())
			.andExpect(jsonPath("$.weekly.range").value("12w"))
			.andExpect(jsonPath("$.weekly.data").isArray())
			.andExpect(jsonPath("$.weekly.data.length()").value(12))
			.andExpect(jsonPath("$.monthly").exists())
			.andExpect(jsonPath("$.monthly.range").value("12m"))
			.andExpect(jsonPath("$.monthly.data").isArray())
			.andExpect(jsonPath("$.monthly.data.length()").value(12));

		System.out.println("통계 조회 성공: 모든 데이터가 0으로 채워짐");
	}

	@Test
	@DisplayName("URL 통계 조회 - 정상 동작 (시간별 데이터 있음)")
	void getUrlAnalytics_정상동작_시간별데이터() throws Exception {
		// Given: Member와 URL 생성
		Member member = new Member(Provider.GOOGLE, "google-hourly", "hourly@example.com");
		Member savedMember = memberRepository.save(member);

		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		Long urlId = saved.getId();

		// 최근 5시간 클릭 히스토리 생성
		LocalDateTime now = LocalDateTime.now();
		for (int i = 0; i < 5; i++) {
			LocalDateTime hour = now.minusHours(i).truncatedTo(ChronoUnit.HOURS);
			ClickCountHistory history = ClickCountHistory.builder()
				.urlMapping(saved)
				.hour(hour)
				.build();
			clickCountHistoryRepository.save(history);
		}

		System.out.println("=== URL 통계 조회 테스트 (시간별 데이터) ===");
		System.out.println("생성된 히스토리 개수: 5");

		// When & Then
		mockMvc.perform(get("/api/v1/my-urls/" + urlId + "/analytics")
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.hourly.data.length()").value(24))
			.andExpect(jsonPath("$.daily.data.length()").value(30))
			.andExpect(jsonPath("$.weekly.data.length()").value(12))
			.andExpect(jsonPath("$.monthly.data.length()").value(12));

		System.out.println("통계 조회 성공: 시간별 데이터 포함");
	}

	@Test
	@DisplayName("URL 통계 조회 - 정상 동작 (일별 데이터 있음)")
	void getUrlAnalytics_정상동작_일별데이터() throws Exception {
		// Given: Member와 URL 생성
		Member member = new Member(Provider.GOOGLE, "google-daily", "daily@example.com");
		Member savedMember = memberRepository.save(member);

		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		Long urlId = saved.getId();

		// 최근 10일 클릭 히스토리 생성
		LocalDateTime now = LocalDateTime.now();
		for (int i = 0; i < 10; i++) {
			LocalDate date = now.toLocalDate().minusDays(i);
			LocalDateTime hour = date.atTime(12, 0);
			ClickCountHistory history = ClickCountHistory.builder()
				.urlMapping(saved)
				.hour(hour)
				.build();
			clickCountHistoryRepository.save(history);
		}

		System.out.println("=== URL 통계 조회 테스트 (일별 데이터) ===");
		System.out.println("생성된 히스토리 개수: 10");

		// When & Then
		mockMvc.perform(get("/api/v1/my-urls/" + urlId + "/analytics")
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.daily.data.length()").value(30));

		System.out.println("통계 조회 성공: 일별 데이터 포함");
	}

	@Test
	@DisplayName("URL 통계 조회 - 정상 동작 (주별 데이터 있음)")
	void getUrlAnalytics_정상동작_주별데이터() throws Exception {
		// Given: Member와 URL 생성
		Member member = new Member(Provider.GOOGLE, "google-weekly", "weekly@example.com");
		Member savedMember = memberRepository.save(member);

		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		Long urlId = saved.getId();

		// 최근 6주 클릭 히스토리 생성
		LocalDateTime now = LocalDateTime.now();
		for (int week = 0; week < 6; week++) {
			LocalDate weekStart = now.toLocalDate()
				.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
				.minusWeeks(week);
			LocalDateTime hour = weekStart.atStartOfDay();
			ClickCountHistory history = ClickCountHistory.builder()
				.urlMapping(saved)
				.hour(hour)
				.build();
			clickCountHistoryRepository.save(history);
		}

		System.out.println("=== URL 통계 조회 테스트 (주별 데이터) ===");
		System.out.println("생성된 히스토리 개수: 6");

		// When & Then
		mockMvc.perform(get("/api/v1/my-urls/" + urlId + "/analytics")
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.weekly.data.length()").value(12));

		System.out.println("통계 조회 성공: 주별 데이터 포함");
	}

	@Test
	@DisplayName("URL 통계 조회 - 정상 동작 (월별 데이터 있음)")
	void getUrlAnalytics_정상동작_월별데이터() throws Exception {
		// Given: Member와 URL 생성
		Member member = new Member(Provider.GOOGLE, "google-monthly", "monthly@example.com");
		Member savedMember = memberRepository.save(member);

		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		Long urlId = saved.getId();

		// 최근 8개월 클릭 히스토리 생성
		LocalDateTime now = LocalDateTime.now();
		for (int month = 0; month < 8; month++) {
			YearMonth targetMonth = YearMonth.from(now).minusMonths(month);
			LocalDateTime hour = targetMonth.atDay(1).atStartOfDay();
			ClickCountHistory history = ClickCountHistory.builder()
				.urlMapping(saved)
				.hour(hour)
				.build();
			clickCountHistoryRepository.save(history);
		}

		System.out.println("=== URL 통계 조회 테스트 (월별 데이터) ===");
		System.out.println("생성된 히스토리 개수: 8");

		// When & Then
		mockMvc.perform(get("/api/v1/my-urls/" + urlId + "/analytics")
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.monthly.data.length()").value(12));

		System.out.println("통계 조회 성공: 월별 데이터 포함");
	}

	@Test
	@DisplayName("URL 통계 조회 - 존재하지 않는 URL")
	void getUrlAnalytics_존재하지않는URL() throws Exception {
		// Given: Member 생성
		Member member = new Member(Provider.GOOGLE, "google-notfound", "notfound@example.com");
		Member savedMember = memberRepository.save(member);

		Long nonExistentUrlId = 999999L;

		System.out.println("=== URL 통계 조회 테스트 (존재하지 않는 URL) ===");
		System.out.println("URL ID: " + nonExistentUrlId);

		// When & Then
		mockMvc.perform(get("/api/v1/my-urls/" + nonExistentUrlId + "/analytics")
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isNotFound());

		System.out.println("예외 발생: 404 NOT_FOUND");
	}

	@Test
	@DisplayName("URL 통계 조회 - 소유자가 아닌 사용자")
	void getUrlAnalytics_소유자가아님() throws Exception {
		// Given: 두 명의 Member 생성
		Member owner = new Member(Provider.GOOGLE, "google-owner", "owner@example.com");
		Member savedOwner = memberRepository.save(owner);

		Member otherMember = new Member(Provider.KAKAO, "kakao-other", "other@example.com");
		Member savedOtherMember = memberRepository.save(otherMember);

		// Owner가 URL 생성
		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.with(SecurityMockMvcRequestPostProcessors.user(savedOwner.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		Long urlId = saved.getId();

		System.out.println("=== URL 통계 조회 테스트 (소유자가 아님) ===");
		System.out.println("URL 소유자 ID: " + savedOwner.getId());
		System.out.println("요청자 ID: " + savedOtherMember.getId());

		// When & Then: 다른 Member가 통계 조회 시도
		mockMvc.perform(get("/api/v1/my-urls/" + urlId + "/analytics")
				.with(SecurityMockMvcRequestPostProcessors.user(savedOtherMember.getId().toString())))
			.andExpect(status().isForbidden());

		System.out.println("예외 발생: 403 FORBIDDEN");
	}

	@Test
	@DisplayName("URL 통계 조회 - 소유자가 없는 URL (익명 URL)")
	void getUrlAnalytics_소유자없는URL() throws Exception {
		// Given: URL을 익명으로 생성
		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		Long urlId = saved.getId();

		// Member 생성 (소유자가 아님)
		Member member = new Member(Provider.GOOGLE, "google-anonymous", "anonymous@example.com");
		Member savedMember = memberRepository.save(member);

		System.out.println("=== URL 통계 조회 테스트 (익명 URL) ===");
		System.out.println("URL 소유자: null");
		System.out.println("요청자 ID: " + savedMember.getId());

		// When & Then: 소유자가 없는 URL의 통계 조회 시도
		mockMvc.perform(get("/api/v1/my-urls/" + urlId + "/analytics")
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isForbidden());

		System.out.println("예외 발생: 403 FORBIDDEN");
	}

	@Test
	@DisplayName("URL 통계 조회 - 인증되지 않은 사용자")
	void getUrlAnalytics_인증되지않음() throws Exception {
		// Given: Member와 URL 생성
		Member member = new Member(Provider.GOOGLE, "google-auth", "auth@example.com");
		Member savedMember = memberRepository.save(member);

		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		Long urlId = saved.getId();

		System.out.println("=== URL 통계 조회 테스트 (인증 없음) ===");
		System.out.println("URL ID: " + urlId);

		// When & Then: 인증 없이 통계 조회 시도
		mockMvc.perform(get("/api/v1/my-urls/" + urlId + "/analytics"))
			.andExpect(status().isUnauthorized());

		System.out.println("예외 발생: 401 UNAUTHORIZED");
	}

	@Test
	@DisplayName("URL 통계 조회 - 복합 데이터 (모든 기간)")
	void getUrlAnalytics_복합데이터() throws Exception {
		// Given: Member와 URL 생성
		Member member = new Member(Provider.GOOGLE, "google-complex", "complex@example.com");
		Member savedMember = memberRepository.save(member);

		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		Long urlId = saved.getId();

		LocalDateTime now = LocalDateTime.now();

		// 최근 12시간 데이터 (시간별로 고유하게, 1~12시간 전)
		for (int i = 1; i <= 12; i++) {
			LocalDateTime hour = now.minusHours(i).truncatedTo(ChronoUnit.HOURS);
			ClickCountHistory history = ClickCountHistory.builder()
				.urlMapping(saved)
				.hour(hour)
				.build();
			clickCountHistoryRepository.save(history);
		}

		// 최근 15일 데이터 (각 날짜 오전 6시로 설정하여 시간별 데이터와 겹치지 않도록)
		for (int i = 1; i <= 15; i++) {
			LocalDate date = now.toLocalDate().minusDays(i);
			LocalDateTime hour = date.atTime(6, 0);
			ClickCountHistory history = ClickCountHistory.builder()
				.urlMapping(saved)
				.hour(hour)
				.build();
			clickCountHistoryRepository.save(history);
		}

		// 최근 8주 데이터 (각 주 월요일 새벽 3시로 설정)
		for (int week = 1; week <= 8; week++) {
			LocalDate weekStart = now.toLocalDate()
				.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
				.minusWeeks(week);
			LocalDateTime hour = weekStart.atTime(3, 0);
			ClickCountHistory history = ClickCountHistory.builder()
				.urlMapping(saved)
				.hour(hour)
				.build();
			clickCountHistoryRepository.save(history);
		}

		// 최근 10개월 데이터 (각 월 15일 오후 9시로 설정)
		for (int month = 1; month <= 10; month++) {
			YearMonth targetMonth = YearMonth.from(now).minusMonths(month);
			LocalDateTime hour = targetMonth.atDay(15).atTime(21, 0);
			ClickCountHistory history = ClickCountHistory.builder()
				.urlMapping(saved)
				.hour(hour)
				.build();
			clickCountHistoryRepository.save(history);
		}

		long totalHistories = clickCountHistoryRepository.count();
		System.out.println("=== URL 통계 조회 테스트 (복합 데이터) ===");
		System.out.println("생성된 총 히스토리 개수: " + totalHistories);

		// When & Then
		mockMvc.perform(get("/api/v1/my-urls/" + urlId + "/analytics")
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.hourly.range").value("24h"))
			.andExpect(jsonPath("$.hourly.data.length()").value(24))
			.andExpect(jsonPath("$.daily.range").value("30d"))
			.andExpect(jsonPath("$.daily.data.length()").value(30))
			.andExpect(jsonPath("$.weekly.range").value("12w"))
			.andExpect(jsonPath("$.weekly.data.length()").value(12))
			.andExpect(jsonPath("$.monthly.range").value("12m"))
			.andExpect(jsonPath("$.monthly.data.length()").value(12));

		System.out.println("통계 조회 성공: 모든 기간의 데이터 포함");
	}

	@Test
	@DisplayName("URL 통계 조회 - Response JSON 구조 검증")
	void getUrlAnalytics_JSON구조검증() throws Exception {
		// Given: Member와 URL 생성
		Member member = new Member(Provider.GOOGLE, "google-json", "json@example.com");
		Member savedMember = memberRepository.save(member);

		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		Long urlId = saved.getId();

		// 몇 개 데이터 생성
		LocalDateTime now = LocalDateTime.now();
		ClickCountHistory history = ClickCountHistory.builder()
			.urlMapping(saved)
			.hour(now.truncatedTo(ChronoUnit.HOURS))
			.build();
		clickCountHistoryRepository.save(history);

		System.out.println("=== URL 통계 조회 테스트 (JSON 구조 검증) ===");

		// When & Then
		mockMvc.perform(get("/api/v1/my-urls/" + urlId + "/analytics")
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.hourly").exists())
			.andExpect(jsonPath("$.hourly.range").isString())
			.andExpect(jsonPath("$.hourly.data").isArray())
			.andExpect(jsonPath("$.hourly.data[0].timestamp").exists())
			.andExpect(jsonPath("$.hourly.data[0].clickCount").isNumber())
			.andExpect(jsonPath("$.daily").exists())
			.andExpect(jsonPath("$.daily.range").isString())
			.andExpect(jsonPath("$.daily.data").isArray())
			.andExpect(jsonPath("$.daily.data[0].date").exists())
			.andExpect(jsonPath("$.daily.data[0].clickCount").isNumber())
			.andExpect(jsonPath("$.weekly").exists())
			.andExpect(jsonPath("$.weekly.range").isString())
			.andExpect(jsonPath("$.weekly.data").isArray())
			.andExpect(jsonPath("$.weekly.data[0].weekStart").exists())
			.andExpect(jsonPath("$.weekly.data[0].clickCount").isNumber())
			.andExpect(jsonPath("$.monthly").exists())
			.andExpect(jsonPath("$.monthly.range").isString())
			.andExpect(jsonPath("$.monthly.data").isArray())
			.andExpect(jsonPath("$.monthly.data[0].yearMonth").exists())
			.andExpect(jsonPath("$.monthly.data[0].clickCount").isNumber());

		System.out.println("JSON 구조 검증 완료");
	}
}
