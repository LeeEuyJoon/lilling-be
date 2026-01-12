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
}
