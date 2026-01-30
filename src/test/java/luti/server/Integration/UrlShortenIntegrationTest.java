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

import luti.server.infrastructure.client.kgs.KgsClient;
import luti.server.infrastructure.client.kgs.KeyBlock;
import luti.server.domain.model.Member;
import luti.server.domain.model.UrlMapping;
import luti.server.domain.enums.Provider;
import luti.server.infrastructure.persistence.MemberRepository;
import luti.server.infrastructure.persistence.UrlMappingRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UrlShortenIntegrationTest {

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
		// MySQL 설정
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
		registry.add("APP_ID", () -> "test-app");

		// Redis 설정
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", redis::getFirstMappedPort);

		// JWT 설정
		registry.add("JWT_SECRET_KEY", () -> "test-secret-key-for-jwt-signing-at-least-32-characters-long");
		registry.add("JWT_ACCESS_TTL_SECONDS", () -> "3600");
		registry.add("JWT_ISSUER", () -> "https://test.lill.ing");
		registry.add("JWT_AUDIENCE", () -> "test.lill.ing");
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

		// KgsClient Mock 설정
		when(kgsClient.fetchNextBlock())
			.thenReturn(new KeyBlock(26001, 27000))
			.thenReturn(new KeyBlock(27001, 2800));
	}

	@Test
	@DisplayName("URL 단축 전체 플로우 - DB 저장까지 검증")
	void shortenUrl_전체플로우_성공() throws Exception {
		// Given
		String originalUrl = "https://www.google.com";
		String requestBody = "{\"originalUrl\":\"" + originalUrl + "\"}";

		// When
		mockMvc.perform(post("/api/v1/url/shorten")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.shortUrl").exists())
			.andExpect(jsonPath("$.shortUrl").isString());

		// Then - DB 검증
		assertEquals(1, urlMappingRepository.count());

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		assertEquals(originalUrl, saved.getOriginalUrl());
		assertNotNull(saved.getId());
		assertNotNull(saved.getKgsId());
		assertNotNull(saved.getScrambledId());
		assertNotNull(saved.getShortUrl());
		assertNotNull(saved.getAppId());
		assertNotNull(saved.getCreatedAt());
		assertEquals("test-app", saved.getAppId());

		System.out.println("=== URL 단축 결과 ===");
		System.out.println("Auto ID: " + saved.getId());
		System.out.println("KGS ID: " + saved.getKgsId());
		System.out.println("Scrambled ID: " + saved.getScrambledId());
		System.out.println("원본 URL: " + saved.getOriginalUrl());
		System.out.println("단축 URL: " + saved.getShortUrl());
		System.out.println("App ID: " + saved.getAppId());
		System.out.println("생성 시각: " + saved.getCreatedAt());
	}

	@Test
	@DisplayName("URL 단축 여러 건 - 각각 고유한 ID 생성 검증")
	void shortenUrl_여러건_고유ID_검증() throws Exception {
		// Given
		String url1 = "https://www.google.com";
		String url2 = "https://www.naver.com";
		String url3 = "https://www.github.com";

		// When
		mockMvc.perform(post("/api/v1/url/shorten")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + url1 + "\"}"));

		mockMvc.perform(post("/api/v1/url/shorten")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + url2 + "\"}"));

		mockMvc.perform(post("/api/v1/url/shorten")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + url3 + "\"}"));

		// Then
		assertEquals(3, urlMappingRepository.count());

		var urls = urlMappingRepository.findAll();

		// Auto-increment ID 검증
		long autoId1 = urls.get(0).getId();
		long autoId2 = urls.get(1).getId();
		long autoId3 = urls.get(2).getId();

		// KGS ID 검증
		long kgsId1 = urls.get(0).getKgsId();
		long kgsId2 = urls.get(1).getKgsId();
		long kgsId3 = urls.get(2).getKgsId();

		// Scrambled ID 검증
		long scrambledId1 = urls.get(0).getScrambledId();
		long scrambledId2 = urls.get(1).getScrambledId();
		long scrambledId3 = urls.get(2).getScrambledId();

		// 모두 다른 ID를 가져야 함
		assertNotEquals(kgsId1, kgsId2);
		assertNotEquals(kgsId2, kgsId3);
		assertNotEquals(kgsId1, kgsId3);

		assertNotEquals(scrambledId1, scrambledId2);
		assertNotEquals(scrambledId2, scrambledId3);
		assertNotEquals(scrambledId1, scrambledId3);

		System.out.println("=== 고유 ID 검증 ===");
		System.out.println("URL1 - Auto ID: " + autoId1 + ", KGS ID: " + kgsId1 + ", Scrambled ID: " + scrambledId1);
		System.out.println("URL2 - Auto ID: " + autoId2 + ", KGS ID: " + kgsId2 + ", Scrambled ID: " + scrambledId2);
		System.out.println("URL3 - Auto ID: " + autoId3 + ", KGS ID: " + kgsId3 + ", Scrambled ID: " + scrambledId3);
	}

	@Test
	@DisplayName("shortCode가 7자 이하인지 검증 (Base62)")
	void shortenUrl_shortCode_길이_검증() throws Exception {
		// Given
		String originalUrl = "https://www.example.com/very/long/path/to/resource";
		String requestBody = "{\"originalUrl\":\"" + originalUrl + "\"}";

		// When
		mockMvc.perform(post("/api/v1/url/shorten")
			.contentType(MediaType.APPLICATION_JSON)
			.content(requestBody));

		// Then
		UrlMapping saved = urlMappingRepository.findAll().get(0);
		String shortUrl = saved.getShortUrl();
		String shortCode = shortUrl.substring(shortUrl.lastIndexOf("/") + 1);

		assertTrue(shortCode.length() <= 7,
			"ShortCode는 7자 이하여야 합니다. 실제: " + shortCode.length() + "자");

		System.out.println("=== Base62 인코딩 검증 ===");
		System.out.println("Short URL: " + shortUrl);
		System.out.println("Short Code: " + shortCode);
		System.out.println("Short Code 길이: " + shortCode.length() + "자");
	}

	@Test
	@DisplayName("리다이렉트 전체 플로우 - 단축 후 조회")
	void redirect_전체플로우_성공() throws Exception {
		// Given: URL 단축
		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		String shortUrl = saved.getShortUrl();
		String shortCode = shortUrl.substring(shortUrl.lastIndexOf("/") + 1);

		// When & Then: 리다이렉트
		mockMvc.perform(get("/" + shortCode))
			.andExpect(status().isFound())
			.andExpect(header().string("Location", originalUrl));

		System.out.println("=== 리다이렉트 검증 ===");
		System.out.println("ShortCode: " + shortCode);
		System.out.println("리다이렉트 대상: " + originalUrl);
	}

	@Test
	@DisplayName("URL 단축 - 인증된 사용자, Member 연결 확인")
	void shortenUrl_인증된사용자_Member_연결() throws Exception {
		// Given: DB에 Member 미리 생성
		Member testMember = new Member(Provider.GOOGLE, "google-123", "test@example.com");
		Member savedMember = memberRepository.save(testMember);

		System.out.println("=== Member 연결 테스트 ===");
		System.out.println("생성된 Member ID: " + savedMember.getId());
		System.out.println("Provider: " + savedMember.getProvider());
		System.out.println("Email: " + savedMember.getEmail());

		String originalUrl = "https://www.example.com/authenticated";

		// When: URL 단축 요청 (인증된 상태)
		// @WithMockUser를 동적으로 설정할 수 없으므로, Security Context를 직접 설정
		mockMvc.perform(post("/api/v1/url/shorten")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"originalUrl\":\"" + originalUrl + "\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.shortUrl").exists());

		// Then: UrlMapping에 Member가 연결되었는지 확인
		assertEquals(1, urlMappingRepository.count());

		UrlMapping savedUrlMapping = urlMappingRepository.findAll().get(0);

		// Lazy Loading 문제 해결: 트랜잭션 내에서 Member 초기화
		Long memberId = savedUrlMapping.getMember() != null ? savedUrlMapping.getMember().getId() : null;

		assertNotNull(memberId, "Member가 null이면 안 됨");
		assertEquals(savedMember.getId(), memberId);

		// Member를 직접 조회해서 검증
		Member actualMember = memberRepository.findById(memberId).orElseThrow();
		assertEquals("test@example.com", actualMember.getEmail());
		assertEquals(Provider.GOOGLE, actualMember.getProvider());

		System.out.println("Member 연결 성공");
		System.out.println("UrlMapping ID: " + savedUrlMapping.getId());
		System.out.println("연결된 Member ID: " + memberId);
		System.out.println("Member Email: " + actualMember.getEmail());
	}

	@Test
	@DisplayName("URL 단축 - 익명 사용자, Member 없음 확인")
	void shortenUrl_익명사용자_Member_없음() throws Exception {
		// Given
		String originalUrl = "https://www.example.com/anonymous";

		System.out.println("=== 익명 사용자 테스트 ===");

		// When: URL 단축 요청 (인증 없음)
		mockMvc.perform(post("/api/v1/url/shorten")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"originalUrl\":\"" + originalUrl + "\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.shortUrl").exists());

		// Then: UrlMapping에 Member가 null인지 확인
		assertEquals(1, urlMappingRepository.count());

		UrlMapping savedUrlMapping = urlMappingRepository.findAll().get(0);
		assertNull(savedUrlMapping.getMember(), "익명 사용자는 Member가 null이어야 함");

		System.out.println("익명 사용자 Member 없음 확인");
		System.out.println("UrlMapping ID: " + savedUrlMapping.getId());
		System.out.println("Member: null");
	}

	@Test
	@DisplayName("URL 단축 - 여러 URL 생성 시 모두 같은 Member 연결")
	void shortenUrl_여러URL_같은Member_연결() throws Exception {
		// Given: DB에 Member 생성
		Member testMember = new Member(Provider.KAKAO, "kakao-999", "kakao@example.com");
		Member savedMember = memberRepository.save(testMember);

		System.out.println("=== 여러 URL 동일 Member 연결 테스트 ===");
		System.out.println("Member ID: " + savedMember.getId());

		String url1 = "https://www.google.com";
		String url2 = "https://www.naver.com";
		String url3 = "https://www.github.com";

		// When: 3개의 URL 단축 (동적으로 Member ID 설정)
		mockMvc.perform(post("/api/v1/url/shorten")
			.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + url1 + "\"}"));

		mockMvc.perform(post("/api/v1/url/shorten")
			.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + url2 + "\"}"));

		mockMvc.perform(post("/api/v1/url/shorten")
			.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + url3 + "\"}"));

		// Then: 3개 모두 같은 Member에 연결되었는지 확인
		assertEquals(3, urlMappingRepository.count());

		var urlMappings = urlMappingRepository.findAll();
		for (UrlMapping mapping : urlMappings) {
			assertNotNull(mapping.getMember());
			assertEquals(savedMember.getId(), mapping.getMember().getId());
		}

		System.out.println("3개 URL 모두 같은 Member에 연결됨");
		System.out.println("생성된 URL 개수: " + urlMappings.size());
	}

}
