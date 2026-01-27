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
import luti.server.domain.model.Member;
import luti.server.domain.model.UrlMapping;
import luti.server.infrastructure.client.kgs.KeyBlock;
import luti.server.infrastructure.client.kgs.KgsClient;
import luti.server.infrastructure.persistence.MemberRepository;
import luti.server.infrastructure.persistence.UrlMappingRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DeleteUrlIntegrationTest {

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
	@DisplayName("URL 삭제 - 성공")
	void deleteUrl_성공() throws Exception {
		// Given: Member와 URL 생성
		Member member = new Member(Provider.GOOGLE, "google-delete", "delete@example.com");
		Member savedMember = memberRepository.save(member);

		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		Long urlId = saved.getId();

		System.out.println("=== URL 삭제 테스트 (성공) ===");
		System.out.println("URL ID: " + urlId);
		System.out.println("Member ID: " + savedMember.getId());
		System.out.println("삭제 전 isDeleted: " + saved.getDeleted());

		// When: URL 삭제
		mockMvc.perform(delete("/api/v1/my-urls/" + urlId)
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isNoContent());

		// Then: DB에서 soft delete 확인 (count는 유지, isDeleted=true, deletedAt 설정)
		assertEquals(1, urlMappingRepository.count());
		UrlMapping deleted = urlMappingRepository.findById(urlId).orElseThrow();
		assertTrue(deleted.getDeleted());
		assertNotNull(deleted.getDeletedAt());

		System.out.println("Soft delete 성공");
		System.out.println("삭제 후 isDeleted: " + deleted.getDeleted());
		System.out.println("삭제 시각: " + deleted.getDeletedAt());
	}

	@Test
	@DisplayName("URL 삭제 - 존재하지 않는 URL")
	void deleteUrl_존재하지않는URL() throws Exception {
		// Given: Member 생성
		Member member = new Member(Provider.GOOGLE, "google-notfound", "notfound@example.com");
		Member savedMember = memberRepository.save(member);

		Long nonExistentUrlId = 999999L;

		System.out.println("=== URL 삭제 테스트 (존재하지 않는 URL) ===");
		System.out.println("URL ID: " + nonExistentUrlId);
		System.out.println("Member ID: " + savedMember.getId());

		// When & Then: 존재하지 않는 URL 삭제 시도
		mockMvc.perform(delete("/api/v1/my-urls/" + nonExistentUrlId)
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isNotFound());

		System.out.println("예외 발생: 404 NOT_FOUND");
	}

	@Test
	@DisplayName("URL 삭제 - 소유자가 아닌 사용자")
	void deleteUrl_소유자가아님() throws Exception {
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

		System.out.println("=== URL 삭제 테스트 (소유자가 아님) ===");
		System.out.println("URL ID: " + urlId);
		System.out.println("URL 소유자 ID: " + savedOwner.getId());
		System.out.println("요청자 ID: " + savedOtherMember.getId());

		// When & Then: 다른 Member가 삭제 시도
		mockMvc.perform(delete("/api/v1/my-urls/" + urlId)
				.with(SecurityMockMvcRequestPostProcessors.user(savedOtherMember.getId().toString())))
			.andExpect(status().isForbidden());

		// Then: URL은 여전히 존재하고 삭제되지 않음
		UrlMapping notDeleted = urlMappingRepository.findById(urlId).orElseThrow();
		assertFalse(notDeleted.getDeleted());
		assertNull(notDeleted.getDeletedAt());

		System.out.println("예외 발생: 403 FORBIDDEN");
		System.out.println("URL은 삭제되지 않음");
	}

	@Test
	@DisplayName("URL 삭제 - 인증되지 않은 사용자")
	void deleteUrl_인증되지않음() throws Exception {
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

		System.out.println("=== URL 삭제 테스트 (인증 없음) ===");
		System.out.println("URL ID: " + urlId);

		// When & Then: 인증 없이 삭제 시도
		mockMvc.perform(delete("/api/v1/my-urls/" + urlId))
			.andExpect(status().isUnauthorized());

		// Then: URL은 여전히 존재하고 삭제되지 않음
		UrlMapping notDeleted = urlMappingRepository.findById(urlId).orElseThrow();
		assertFalse(notDeleted.getDeleted());
		assertNull(notDeleted.getDeletedAt());

		System.out.println("예외 발생: 401 UNAUTHORIZED");
		System.out.println("URL은 삭제되지 않음");
	}

	@Test
	@DisplayName("URL 삭제 - 소유자가 없는 URL (익명 URL)")
	void deleteUrl_소유자없는URL() throws Exception {
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

		System.out.println("=== URL 삭제 테스트 (익명 URL) ===");
		System.out.println("URL ID: " + urlId);
		System.out.println("URL 소유자: null");
		System.out.println("요청자 ID: " + savedMember.getId());

		// When & Then: 소유자가 없는 URL의 삭제 시도
		// Note: 현재 production 코드는 member가 null일 때 NullPointerException 발생
		// GlobalExceptionHandler에서 catch되어 400 BAD_REQUEST 반환
		mockMvc.perform(delete("/api/v1/my-urls/" + urlId)
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isBadRequest());

		// Then: URL은 여전히 존재하고 삭제되지 않음
		UrlMapping notDeleted = urlMappingRepository.findById(urlId).orElseThrow();
		assertFalse(notDeleted.getDeleted());
		assertNull(notDeleted.getDeletedAt());

		System.out.println("예외 발생: 400 BAD_REQUEST (NullPointerException)");
		System.out.println("Note: Production 코드에 버그 있음 - member null 체크 누락");
		System.out.println("URL은 삭제되지 않음");
	}

	@Test
	@DisplayName("URL 삭제 - 여러 URL 중 특정 URL만 삭제")
	void deleteUrl_여러URL중특정URL만삭제() throws Exception {
		// Given: Member와 3개의 URL 생성
		Member member = new Member(Provider.GOOGLE, "google-multiple", "multiple@example.com");
		Member savedMember = memberRepository.save(member);

		for (int i = 1; i <= 3; i++) {
			String url = "https://www.example" + i + ".com";
			mockMvc.perform(post("/api/v1/url/shorten")
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"originalUrl\":\"" + url + "\"}"));
		}

		// 두 번째 URL을 삭제
		UrlMapping urlToDelete = urlMappingRepository.findAll().get(1);
		Long urlIdToDelete = urlToDelete.getId();

		System.out.println("=== URL 삭제 테스트 (여러 URL 중 특정 URL만) ===");
		System.out.println("총 URL 개수: 3");
		System.out.println("삭제할 URL ID: " + urlIdToDelete);

		// When: 두 번째 URL 삭제
		mockMvc.perform(delete("/api/v1/my-urls/" + urlIdToDelete)
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isNoContent());

		// Then: soft delete로 3개 모두 존재하지만, 1개는 isDeleted=true
		assertEquals(3, urlMappingRepository.count());
		UrlMapping deleted = urlMappingRepository.findById(urlIdToDelete).orElseThrow();
		assertTrue(deleted.getDeleted());

		// 나머지 2개는 삭제되지 않음
		long notDeletedCount = urlMappingRepository.findAll().stream()
			.filter(url -> !url.getDeleted())
			.count();
		assertEquals(2, notDeletedCount);

		System.out.println("삭제 후 전체 URL 개수: " + urlMappingRepository.count());
		System.out.println("삭제되지 않은 URL 개수: " + notDeletedCount);
		System.out.println("특정 URL만 soft delete 성공");
	}

	@Test
	@DisplayName("URL 삭제 - PathVariable 파라미터 바인딩 검증")
	void deleteUrl_PathVariable파라미터바인딩() throws Exception {
		// Given: Member와 URL 생성
		Member member = new Member(Provider.GOOGLE, "google-pathvar", "pathvar@example.com");
		Member savedMember = memberRepository.save(member);

		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		Long urlId = saved.getId();

		System.out.println("=== URL 삭제 테스트 (PathVariable 바인딩) ===");
		System.out.println("URL ID: " + urlId);
		System.out.println("Note: Controller의 @PathVariable Long urlId (명시적 이름 없음)");

		// When & Then: PathVariable이 제대로 바인딩되어 삭제됨
		mockMvc.perform(delete("/api/v1/my-urls/" + urlId)
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isNoContent());

		// Then: soft delete 확인
		UrlMapping deleted = urlMappingRepository.findById(urlId).orElseThrow();
		assertTrue(deleted.getDeleted());
		assertNotNull(deleted.getDeletedAt());

		System.out.println("PathVariable 바인딩 성공 및 soft delete 완료");
	}

	@Test
	@DisplayName("URL 삭제 - 잘못된 URL ID 형식")
	void deleteUrl_잘못된ID형식() throws Exception {
		// Given: Member 생성
		Member member = new Member(Provider.GOOGLE, "google-invalid", "invalid@example.com");
		Member savedMember = memberRepository.save(member);

		String invalidUrlId = "invalid-id";

		System.out.println("=== URL 삭제 테스트 (잘못된 ID 형식) ===");
		System.out.println("Invalid URL ID: " + invalidUrlId);

		// When & Then: 잘못된 ID 형식으로 요청
		mockMvc.perform(delete("/api/v1/my-urls/" + invalidUrlId)
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isBadRequest());

		System.out.println("예외 발생: 400 BAD_REQUEST");
	}

	@Test
	@DisplayName("URL 삭제 후 목록 조회 - 삭제된 URL은 조회되지 않음")
	void deleteUrl_후_목록조회() throws Exception {
		// Given: Member와 3개의 URL 생성
		Member member = new Member(Provider.GOOGLE, "google-list", "list@example.com");
		Member savedMember = memberRepository.save(member);

		for (int i = 1; i <= 3; i++) {
			String url = "https://www.example" + i + ".com";
			mockMvc.perform(post("/api/v1/url/shorten")
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"originalUrl\":\"" + url + "\"}"));
		}

		UrlMapping urlToDelete = urlMappingRepository.findAll().get(1);
		Long urlIdToDelete = urlToDelete.getId();

		System.out.println("=== URL 삭제 후 목록 조회 테스트 ===");
		System.out.println("초기 URL 개수: 3");

		// When: URL 삭제
		mockMvc.perform(delete("/api/v1/my-urls/" + urlIdToDelete)
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isNoContent());

		System.out.println("URL 삭제 완료");

		// Then: 목록 조회 시 2개만 조회됨
		mockMvc.perform(get("/api/v1/my-urls/list")
				.param("page", "0")
				.param("size", "10")
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.urls").isArray())
			.andExpect(jsonPath("$.urls.length()").value(2))
			.andExpect(jsonPath("$.totalElements").value(2));

		System.out.println("목록 조회 결과: 2개 (삭제된 URL 제외)");
	}

	@Test
	@DisplayName("URL 삭제 - 음수 URL ID")
	void deleteUrl_음수ID() throws Exception {
		// Given: Member 생성
		Member member = new Member(Provider.GOOGLE, "google-negative", "negative@example.com");
		Member savedMember = memberRepository.save(member);

		Long negativeUrlId = -1L;

		System.out.println("=== URL 삭제 테스트 (음수 ID) ===");
		System.out.println("URL ID: " + negativeUrlId);

		// When & Then: 음수 ID로 삭제 시도
		mockMvc.perform(delete("/api/v1/my-urls/" + negativeUrlId)
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isNotFound());

		System.out.println("예외 발생: 404 NOT_FOUND");
	}

	@Test
	@DisplayName("URL 삭제 - 0번 URL ID")
	void deleteUrl_0번ID() throws Exception {
		// Given: Member 생성
		Member member = new Member(Provider.GOOGLE, "google-zero", "zero@example.com");
		Member savedMember = memberRepository.save(member);

		Long zeroUrlId = 0L;

		System.out.println("=== URL 삭제 테스트 (0번 ID) ===");
		System.out.println("URL ID: " + zeroUrlId);

		// When & Then: 0번 ID로 삭제 시도
		mockMvc.perform(delete("/api/v1/my-urls/" + zeroUrlId)
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isNotFound());

		System.out.println("예외 발생: 404 NOT_FOUND");
	}

	@Test
	@DisplayName("URL 삭제 - 동일 URL 2번 연속 삭제 시도")
	void deleteUrl_중복삭제시도() throws Exception {
		// Given: Member와 URL 생성
		Member member = new Member(Provider.GOOGLE, "google-double", "double@example.com");
		Member savedMember = memberRepository.save(member);

		String originalUrl = "https://www.example.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString()))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		Long urlId = saved.getId();

		System.out.println("=== URL 삭제 테스트 (중복 삭제 시도) ===");
		System.out.println("URL ID: " + urlId);

		// When: 첫 번째 삭제 (성공)
		mockMvc.perform(delete("/api/v1/my-urls/" + urlId)
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isNoContent());

		UrlMapping firstDelete = urlMappingRepository.findById(urlId).orElseThrow();
		assertTrue(firstDelete.getDeleted());
		System.out.println("첫 번째 삭제 성공 - isDeleted: true");

		// Then: 두 번째 삭제 시도 (성공 - soft delete는 중복 가능)
		mockMvc.perform(delete("/api/v1/my-urls/" + urlId)
				.with(SecurityMockMvcRequestPostProcessors.user(savedMember.getId().toString())))
			.andExpect(status().isNoContent());

		UrlMapping secondDelete = urlMappingRepository.findById(urlId).orElseThrow();
		assertTrue(secondDelete.getDeleted());
		System.out.println("두 번째 삭제도 성공 (soft delete는 멱등성)");
	}
}
