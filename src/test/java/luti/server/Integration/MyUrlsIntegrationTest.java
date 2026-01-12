package luti.server.Integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import luti.server.client.KgsClient;
import luti.server.client.dto.KeyBlock;
import luti.server.entity.Member;
import luti.server.entity.UrlMapping;
import luti.server.enums.Provider;
import luti.server.repository.MemberRepository;
import luti.server.repository.UrlMappingRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MyUrlsIntegrationTest {

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
		registry.add("JWT_ISSUER", () -> "https://api.lill.ing");
		registry.add("JWT_AUDIENCE", () -> "api.lill.ing");

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

	@BeforeEach
	void setUp() {
		urlMappingRepository.deleteAll();
		memberRepository.deleteAll();

		when(kgsClient.fetchNextBlock())
			.thenReturn(new KeyBlock(26001, 27000))
			.thenReturn(new KeyBlock(27001, 28000));
	}

	@Test
	@DisplayName("URL 검증 - 유효한 URL (소유자 없음)")
	void verify_유효한URL_소유자없음() throws Exception {
		// Given: Member 생성
		Member member = new Member(Provider.GOOGLE, "google-verify", "verify@example.com");
		Member savedMember = memberRepository.save(member);

		// URL 단축 (익명)
		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		String shortUrl = saved.getShortUrl();

		System.out.println("=== URL 검증 테스트 (유효한 URL) ===");
		System.out.println("Short URL: " + shortUrl);

		// When & Then: verify 요청 (인증 필요)
		mockMvc.perform(get("/api/v1/my-urls/verify?shortUrl=" + shortUrl)
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(true))
			.andExpect(jsonPath("$.originalUrl").value(originalUrl))
			.andExpect(jsonPath("$.shortUrl").value(shortUrl))
			.andExpect(jsonPath("$.clickCount").value(0))
			.andExpect(jsonPath("$.createdAt").exists());

		System.out.println("검증 결과: valid=true (클레임 가능)");
	}

	@Test
	@DisplayName("URL 검증 - 잘못된 형식")
	void verify_잘못된형식() throws Exception {
		// Given: Member 생성
		Member member = new Member(Provider.GOOGLE, "google-invalid", "invalid@example.com");
		Member savedMember = memberRepository.save(member);

		String invalidUrl = "invalid-url-format";

		System.out.println("=== URL 검증 테스트 (잘못된 형식) ===");
		System.out.println("Invalid URL: " + invalidUrl);

		// When & Then
		mockMvc.perform(get("/api/v1/my-urls/verify?shortUrl=" + invalidUrl)
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(false))
			.andExpect(jsonPath("$.originalUrl").doesNotExist())
			.andExpect(jsonPath("$.shortUrl").doesNotExist())
			.andExpect(jsonPath("$.clickCount").doesNotExist());

		System.out.println("검증 결과: valid=false (형식 오류)");
	}

	@Test
	@DisplayName("URL 검증 - 존재하지 않는 URL")
	void verify_존재하지않는URL() throws Exception {
		// Given: Member 생성
		Member member = new Member(Provider.GOOGLE, "google-notfound", "notfound@example.com");
		Member savedMember = memberRepository.save(member);

		String nonExistentUrl = "lill.ing/ZZZZZZZ";

		System.out.println("=== URL 검증 테스트 (존재하지 않음) ===");
		System.out.println("Non-existent URL: " + nonExistentUrl);

		// When & Then
		mockMvc.perform(get("/api/v1/my-urls/verify?shortUrl=" + nonExistentUrl)
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(false))
			.andExpect(jsonPath("$.originalUrl").doesNotExist());

		System.out.println("검증 결과: valid=false (존재하지 않음)");
	}

	@Test
	@DisplayName("URL 검증 - 이미 소유자가 있는 URL")
	void verify_이미소유자있음() throws Exception {
		// Given: 소유자 Member 생성
		Member owner = new Member(Provider.GOOGLE, "google-owner2", "owner2@example.com");
		Member savedOwner = memberRepository.save(owner);

		// 검증 요청할 Member 생성
		Member verifier = new Member(Provider.KAKAO, "kakao-verifier", "verifier@example.com");
		Member savedVerifier = memberRepository.save(verifier);

		// URL 단축 (인증된 사용자)
		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedOwner.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		String shortUrl = saved.getShortUrl();

		System.out.println("=== URL 검증 테스트 (이미 소유자 있음) ===");
		System.out.println("Short URL: " + shortUrl);
		System.out.println("Owner Member ID: " + savedOwner.getId());

		// When & Then: 다른 Member가 verify 요청
		mockMvc.perform(get("/api/v1/my-urls/verify?shortUrl=" + shortUrl)
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedVerifier.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(false))
			.andExpect(jsonPath("$.originalUrl").doesNotExist());

		System.out.println("검증 결과: valid=false (이미 소유자 있음)");
	}

	@Test
	@DisplayName("URL 클레임 - 성공")
	void claim_성공() throws Exception {
		// Given: Member 생성
		Member member = new Member(Provider.GOOGLE, "google-456", "claim@example.com");
		Member savedMember = memberRepository.save(member);

		// URL 단축 (익명)
		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		String shortUrl = saved.getShortUrl();

		System.out.println("=== URL 클레임 테스트 (성공) ===");
		System.out.println("Short URL: " + shortUrl);
		System.out.println("Member ID: " + savedMember.getId());
		System.out.println("클레임 전 소유자: " + saved.getMember());

		// When: claim 요청
		mockMvc.perform(post("/api/v1/my-urls/claim")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"shortUrl\":\"" + shortUrl + "\"}"))
			.andExpect(status().isNoContent());

		// Then: DB 확인
		UrlMapping claimed = urlMappingRepository.findById(saved.getId()).orElseThrow();
		assertNotNull(claimed.getMember());
		assertEquals(savedMember.getId(), claimed.getMember().getId());

		System.out.println("클레임 성공");
		System.out.println("클레임 후 소유자: Member ID=" + claimed.getMember().getId());
	}

	@Test
	@DisplayName("URL 클레임 - 잘못된 형식")
	void claim_잘못된형식() throws Exception {
		// Given: Member 생성
		Member member = new Member(Provider.GOOGLE, "google-789", "claim2@example.com");
		Member savedMember = memberRepository.save(member);

		String invalidUrl = "invalid-url-format";

		System.out.println("=== URL 클레임 테스트 (잘못된 형식) ===");
		System.out.println("Invalid URL: " + invalidUrl);

		// When & Then
		mockMvc.perform(post("/api/v1/my-urls/claim")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"shortUrl\":\"" + invalidUrl + "\"}"))
			.andExpect(status().isBadRequest());

		System.out.println("클레임 실패: 잘못된 형식");
	}

	@Test
	@DisplayName("URL 클레임 - 존재하지 않는 URL")
	void claim_존재하지않는URL() throws Exception {
		// Given: Member 생성
		Member member = new Member(Provider.GOOGLE, "google-999", "claim3@example.com");
		Member savedMember = memberRepository.save(member);

		String nonExistentUrl = "lill.ing/ZZZZZZZ";

		System.out.println("=== URL 클레임 테스트 (존재하지 않음) ===");
		System.out.println("Non-existent URL: " + nonExistentUrl);

		// When & Then
		mockMvc.perform(post("/api/v1/my-urls/claim")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"shortUrl\":\"" + nonExistentUrl + "\"}"))
			.andExpect(status().isBadRequest());

		System.out.println("클레임 실패: 존재하지 않음");
	}

	@Test
	@DisplayName("URL 클레임 - 이미 소유자가 있는 URL")
	void claim_이미소유자있음() throws Exception {
		// Given: 첫 번째 Member 생성 (원래 소유자)
		Member owner = new Member(Provider.GOOGLE, "google-owner", "owner@example.com");
		Member savedOwner = memberRepository.save(owner);

		// URL 단축 (첫 번째 Member)
		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedOwner.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		String shortUrl = saved.getShortUrl();

		// 두 번째 Member 생성 (클레임 시도자)
		Member claimer = new Member(Provider.KAKAO, "kakao-claimer", "claimer@example.com");
		Member savedClaimer = memberRepository.save(claimer);

		System.out.println("=== URL 클레임 테스트 (이미 소유자 있음) ===");
		System.out.println("Short URL: " + shortUrl);
		System.out.println("원래 소유자: Member ID=" + savedOwner.getId());
		System.out.println("클레임 시도자: Member ID=" + savedClaimer.getId());

		// When & Then: 두 번째 Member가 클레임 시도
		mockMvc.perform(post("/api/v1/my-urls/claim")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedClaimer.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"shortUrl\":\"" + shortUrl + "\"}"))
			.andExpect(status().isConflict());

		// Then: 소유자는 여전히 원래 소유자
		UrlMapping stillOwned = urlMappingRepository.findById(saved.getId()).orElseThrow();
		assertEquals(savedOwner.getId(), stillOwned.getMember().getId());

		System.out.println("클레임 실패: 이미 소유자 있음");
		System.out.println("현재 소유자: Member ID=" + stillOwned.getMember().getId());
	}

	@Test
	@DisplayName("verify 후 claim 전체 플로우")
	void verify_후_claim_전체플로우() throws Exception {
		// Given: Member 생성
		Member member = new Member(Provider.GOOGLE, "google-flow", "flow@example.com");
		Member savedMember = memberRepository.save(member);

		// URL 단축 (익명)
		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		String shortUrl = saved.getShortUrl();

		System.out.println("=== verify → claim 전체 플로우 ===");
		System.out.println("Short URL: " + shortUrl);

		// When 1: verify 요청 (클레임 가능 확인)
		mockMvc.perform(get("/api/v1/my-urls/verify?shortUrl=" + shortUrl)
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(true));

		System.out.println("Step 1: verify 성공 (클레임 가능)");

		// When 2: claim 요청
		mockMvc.perform(post("/api/v1/my-urls/claim")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"shortUrl\":\"" + shortUrl + "\"}"))
			.andExpect(status().isNoContent());

		System.out.println("Step 2: claim 성공");

		// Then: verify 다시 요청
		mockMvc.perform(get("/api/v1/my-urls/verify?shortUrl=" + shortUrl)
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.valid").value(false));

		System.out.println("Step 3: 재검증 - valid=false (이미 클레임됨)");

		// DB 확인
		UrlMapping claimed = urlMappingRepository.findById(saved.getId()).orElseThrow();
		assertEquals(savedMember.getId(), claimed.getMember().getId());

		System.out.println("전체 플로우 완료");
		System.out.println("최종 소유자: Member ID=" + claimed.getMember().getId());
	}

	@Test
	@DisplayName("URL 목록 조회 - 빈 리스트")
	void list_빈리스트() throws Exception {
		// Given: Member 생성
		Member member = new Member(Provider.GOOGLE, "google-list", "list@example.com");
		Member savedMember = memberRepository.save(member);

		System.out.println("=== URL 목록 조회 테스트 (빈 리스트) ===");
		System.out.println("Member ID: " + savedMember.getId());

		// When & Then: list 요청
		mockMvc.perform(get("/api/v1/my-urls/list")
							.param("page", "0")
							.param("size", "10")
							.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.urls").isArray())
			   .andExpect(jsonPath("$.urls").isEmpty())
			   .andExpect(jsonPath("$.totalElements").value(0))
			   .andExpect(jsonPath("$.totalPages").value(0))
			   .andExpect(jsonPath("$.currentPage").value(0))
			   .andExpect(jsonPath("$.pageSize").value(10));

		System.out.println("빈 리스트 조회 성공");
	}

	@Test
	@DisplayName("URL 목록 조회 - 단일 URL")
	void list_단일URL() throws Exception {
		// Given: Member 생성
		Member member = new Member(Provider.GOOGLE, "google-list-single", "list-single@example.com");
		Member savedMember = memberRepository.save(member);

		// URL 단축
		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		// URL claim
		UrlMapping saved = urlMappingRepository.findAll().get(0);
		String shortUrl = saved.getShortUrl();

		mockMvc.perform(post("/api/v1/my-urls/claim")
							.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"shortUrl\":\"" + shortUrl + "\"}"));

		System.out.println("=== URL 목록 조회 테스트 (단일 URL) ===");
		System.out.println("Short URL: " + shortUrl);

		// When & Then: list 요청
		mockMvc.perform(get("/api/v1/my-urls/list")
							.param("page", "0")
							.param("size", "10")
							.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.urls").isArray())
			   .andExpect(jsonPath("$.urls.length()").value(1))
			   .andExpect(jsonPath("$.urls[0].id").exists())
			   .andExpect(jsonPath("$.urls[0].shortUrl").value(shortUrl))
			   .andExpect(jsonPath("$.urls[0].originalUrl").value(originalUrl))
			   .andExpect(jsonPath("$.urls[0].clickCount").value(0))
			   .andExpect(jsonPath("$.urls[0].createdAt").exists())
			   .andExpect(jsonPath("$.totalElements").value(1))
			   .andExpect(jsonPath("$.totalPages").value(1))
			   .andExpect(jsonPath("$.currentPage").value(0))
			   .andExpect(jsonPath("$.pageSize").value(10));

		System.out.println("단일 URL 조회 성공");
	}

	@Test
	@DisplayName("URL 목록 조회 - 여러 URL (페이지네이션)")
	void list_여러URL_페이지네이션() throws Exception {
		// Given: Member 생성
		Member member = new Member(Provider.GOOGLE, "google-list-multi", "list-multi@example.com");
		Member savedMember = memberRepository.save(member);

		// 15개 URL 생성 및 claim
		for (int i = 1; i <= 15; i++) {
			String url = "https://www.example" + i + ".com";
			mockMvc.perform(post("/api/v1/url/shorten")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"originalUrl\":\"" + url + "\"}"));

			UrlMapping saved = urlMappingRepository.findAll().get(i - 1);
			mockMvc.perform(post("/api/v1/my-urls/claim")
								.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"shortUrl\":\"" + saved.getShortUrl() + "\"}"));
		}

		System.out.println("=== URL 목록 조회 테스트 (페이지네이션) ===");
		System.out.println("총 URL 개수: 15");

		// When & Then 1: 첫 페이지 (0-9)
		mockMvc.perform(get("/api/v1/my-urls/list")
							.param("page", "0")
							.param("size", "10")
							.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.urls.length()").value(10))
			   .andExpect(jsonPath("$.totalElements").value(15))
			   .andExpect(jsonPath("$.totalPages").value(2))
			   .andExpect(jsonPath("$.currentPage").value(0))
			   .andExpect(jsonPath("$.pageSize").value(10));

		System.out.println("첫 페이지 조회 성공: 10개");

		// When & Then 2: 두 번째 페이지 (10-14)
		mockMvc.perform(get("/api/v1/my-urls/list")
							.param("page", "1")
							.param("size", "10")
							.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.urls.length()").value(5))
			   .andExpect(jsonPath("$.totalElements").value(15))
			   .andExpect(jsonPath("$.totalPages").value(2))
			   .andExpect(jsonPath("$.currentPage").value(1))
			   .andExpect(jsonPath("$.pageSize").value(10));

		System.out.println("두 번째 페이지 조회 성공: 5개");
	}

	@Test
	@DisplayName("URL 목록 조회 - 다른 사용자 URL은 조회되지 않음")
	void list_다른사용자URL제외() throws Exception {
		// Given: 두 명의 Member 생성
		Member member1 = new Member(Provider.GOOGLE, "google-list-1", "list1@example.com");
		Member savedMember1 = memberRepository.save(member1);

		Member member2 = new Member(Provider.GOOGLE, "google-list-2", "list2@example.com");
		Member savedMember2 = memberRepository.save(member2);

		// Member1이 2개 URL claim
		for (int i = 1; i <= 2; i++) {
			String url = "https://member1-url" + i + ".com";
			mockMvc.perform(post("/api/v1/url/shorten")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"originalUrl\":\"" + url + "\"}"));

			UrlMapping saved = urlMappingRepository.findAll().get(i - 1);
			mockMvc.perform(post("/api/v1/my-urls/claim")
								.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember1.getId().toString()))
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"shortUrl\":\"" + saved.getShortUrl() + "\"}"));
		}

		// Member2가 3개 URL claim
		for (int i = 1; i <= 3; i++) {
			String url = "https://member2-url" + i + ".com";
			mockMvc.perform(post("/api/v1/url/shorten")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"originalUrl\":\"" + url + "\"}"));

			UrlMapping saved = urlMappingRepository.findAll().get(i + 1); // offset by member1's URLs
			mockMvc.perform(post("/api/v1/my-urls/claim")
								.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember2.getId().toString()))
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"shortUrl\":\"" + saved.getShortUrl() + "\"}"));
		}

		System.out.println("=== URL 목록 조회 테스트 (사용자 분리) ===");
		System.out.println("Member1 URL: 2개, Member2 URL: 3개");

		// When & Then 1: Member1은 2개만 조회
		mockMvc.perform(get("/api/v1/my-urls/list")
							.param("page", "0")
							.param("size", "10")
							.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember1.getId().toString())))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.urls.length()").value(2))
			   .andExpect(jsonPath("$.totalElements").value(2));

		System.out.println("Member1 조회 성공: 2개");

		// When & Then 2: Member2는 3개만 조회
		mockMvc.perform(get("/api/v1/my-urls/list")
							.param("page", "0")
							.param("size", "10")
							.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember2.getId().toString())))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.urls.length()").value(3))
			   .andExpect(jsonPath("$.totalElements").value(3));

		System.out.println("Member2 조회 성공: 3개");
	}

	@Test
	@DisplayName("URL 목록 조회 - 최신순 정렬 확인")
	void list_최신순정렬() throws Exception {
		// Given: Member 생성
		Member member = new Member(Provider.GOOGLE, "google-list-order", "list-order@example.com");
		Member savedMember = memberRepository.save(member);

		// 3개 URL 생성 (시간 간격을 두고)
		String[] urls = {
			"https://first.com",
			"https://second.com",
			"https://third.com"
		};

		String[] shortUrls = new String[3];

		for (int i = 0; i < 3; i++) {
			mockMvc.perform(post("/api/v1/url/shorten")
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"originalUrl\":\"" + urls[i] + "\"}"));

			UrlMapping saved = urlMappingRepository.findAll().get(i);
			shortUrls[i] = saved.getShortUrl();

			mockMvc.perform(post("/api/v1/my-urls/claim")
								.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
								.contentType(MediaType.APPLICATION_JSON)
								.content("{\"shortUrl\":\"" + shortUrls[i] + "\"}"));

			Thread.sleep(100); // 시간 차이 보장
		}

		System.out.println("=== URL 목록 조회 테스트 (최신순 정렬) ===");

		// When & Then: 최신 URL이 먼저 조회되어야 함
		mockMvc.perform(get("/api/v1/my-urls/list")
							.param("page", "0")
							.param("size", "10")
							.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			   .andExpect(status().isOk())
			   .andExpect(jsonPath("$.urls[0].originalUrl").value(urls[2]))  // 최신
			   .andExpect(jsonPath("$.urls[1].originalUrl").value(urls[1]))
			   .andExpect(jsonPath("$.urls[2].originalUrl").value(urls[0])); // 최초

		System.out.println("최신순 정렬 확인 완료");
	}
}
