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
import luti.server.domain.model.UrlMapping;
import luti.server.infrastructure.persistence.UrlMappingRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RedirectIntegrationTest {

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
		registry.add("JWT_ISSUER", () -> "https://api.lill.ing");
		registry.add("JWT_AUDIENCE", () -> "api.lill.ing");
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private KgsClient kgsClient;

	@Autowired
	private UrlMappingRepository urlMappingRepository;

	@BeforeEach
	void setUp() {
		urlMappingRepository.deleteAll();

		// KgsClient Mock 설정
		when(kgsClient.fetchNextBlock())
			.thenReturn(new KeyBlock(26001, 27000))
			.thenReturn(new KeyBlock(27001, 28000));
	}

	@Test
	@DisplayName("리다이렉트 - 302 응답 및 Location 헤더 검증")
	void redirect_정상동작() throws Exception {
		// Given: URL 단축
		String originalUrl = "https://www.example.com/test";
		mockMvc.perform(post("/api/v1/url/shorten")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		String shortUrl = saved.getShortUrl();
		String shortCode = shortUrl.substring(shortUrl.lastIndexOf("/") + 1);

		System.out.println("=== 리다이렉트 테스트 준비 ===");
		System.out.println("원본 URL: " + originalUrl);
		System.out.println("단축 URL: " + shortUrl);
		System.out.println("ShortCode: " + shortCode);

		// When & Then: 리다이렉트
		mockMvc.perform(get("/" + shortCode))
			.andExpect(status().isFound())  // 302 FOUND
			.andExpect(header().string("Location", originalUrl));

		System.out.println("✅ 리다이렉트 성공 (302 FOUND)");
	}

	@Test
	@DisplayName("리다이렉트 - 클릭 횟수 증가 검증 (비동기)")
	void redirect_클릭횟수증가() throws Exception {
		// Given: URL 단축
		String originalUrl = "https://www.naver.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		String shortUrl = saved.getShortUrl();
		String shortCode = shortUrl.substring(shortUrl.lastIndexOf("/") + 1);
		Long initialClickCount = saved.getClickCount();

		System.out.println("=== 클릭 횟수 증가 테스트 ===");
		System.out.println("초기 클릭 횟수: " + initialClickCount);

		// When: 리다이렉트 호출
		mockMvc.perform(get("/" + shortCode))
			.andExpect(status().isFound());

		// 비동기 처리 대기
		Thread.sleep(1000);

		// Then: DB에서 클릭 횟수 확인
		UrlMapping updated = urlMappingRepository.findById(saved.getId())
			.orElseThrow(() -> new AssertionError("엔티티를 찾을 수 없음"));

		System.out.println("업데이트된 클릭 횟수: " + updated.getClickCount());

		assertEquals(initialClickCount + 1, updated.getClickCount(),
			"클릭 횟수가 1 증가해야 함");

		System.out.println("✅ 클릭 횟수 정상 증가");
	}

	@Test
	@DisplayName("리다이렉트 - 여러 번 호출 시 클릭 횟수 누적")
	void redirect_여러번호출_카운트누적() throws Exception {
		// Given: URL 단축
		String originalUrl = "https://www.google.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		String shortUrl = saved.getShortUrl();
		String shortCode = shortUrl.substring(shortUrl.lastIndexOf("/") + 1);
		Long initialClickCount = saved.getClickCount();
		int clickTimes = 5;

		System.out.println("=== 여러 번 클릭 테스트 ===");
		System.out.println("초기 클릭 횟수: " + initialClickCount);
		System.out.println("요청 횟수: " + clickTimes + "번");

		// When: 여러 번 리다이렉트 호출
		for (int i = 0; i < clickTimes; i++) {
			mockMvc.perform(get("/" + shortCode))
				.andExpect(status().isFound());
		}

		// 비동기 처리 대기
		Thread.sleep(2000);

		// Then: DB에서 클릭 횟수 확인
		UrlMapping updated = urlMappingRepository.findById(saved.getId())
			.orElseThrow(() -> new AssertionError("엔티티를 찾을 수 없음"));

		System.out.println("업데이트된 클릭 횟수: " + updated.getClickCount());

		assertEquals(initialClickCount + clickTimes, updated.getClickCount(),
			"클릭 횟수가 " + clickTimes + "번 증가해야 함");

		System.out.println("✅ 클릭 횟수 정상 누적 (" + clickTimes + "번)");
	}

	@Test
	@DisplayName("리다이렉트 - 존재하지 않는 shortCode 요청 시 예외")
	void redirect_존재하지않는URL() throws Exception {
		// Given: 존재하지 않는 shortCode
		String nonExistentShortCode = "zzz9999";

		System.out.println("=== 존재하지 않는 URL 테스트 ===");
		System.out.println("ShortCode: " + nonExistentShortCode);

		// When & Then: 예외 발생 확인 (JSON 응답)
		mockMvc.perform(get("/" + nonExistentShortCode)
				.accept(MediaType.APPLICATION_JSON))  // JSON 응답 기대
			.andExpect(status().isNotFound())  // 404 NOT FOUND
			.andExpect(jsonPath("$.code").value("URL_NOT_FOUND"))
			.andExpect(jsonPath("$.message").exists());

		System.out.println("✅ 존재하지 않는 URL 예외 처리 확인 (404 + JSON 응답)");
	}

	@Test
	@DisplayName("리다이렉트 - 동시 요청 시 클릭 횟수 정확성 검증 (레이스 컨디션)")
	void redirect_동시요청_레이스컨디션() throws Exception {
		// Given: URL 단축
		String originalUrl = "https://www.github.com";
		mockMvc.perform(post("/api/v1/url/shorten")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"originalUrl\":\"" + originalUrl + "\"}"));

		UrlMapping saved = urlMappingRepository.findAll().get(0);
		String shortUrl = saved.getShortUrl();
		String shortCode = shortUrl.substring(shortUrl.lastIndexOf("/") + 1);
		Long initialClickCount = saved.getClickCount();
		int concurrentRequests = 10;

		System.out.println("=== 동시 요청 레이스 컨디션 테스트 ===");
		System.out.println("초기 클릭 횟수: " + initialClickCount);
		System.out.println("동시 요청 횟수: " + concurrentRequests + "번");

		// When: 동시에 여러 요청 (병렬 처리)
		Thread[] threads = new Thread[concurrentRequests];
		for (int i = 0; i < concurrentRequests; i++) {
			threads[i] = new Thread(() -> {
				try {
					mockMvc.perform(get("/" + shortCode))
						.andExpect(status().isFound());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			threads[i].start();
		}

		// 모든 스레드 완료 대기
		for (Thread thread : threads) {
			thread.join();
		}

		// 비동기 처리 대기
		Thread.sleep(3000);

		// Then: 정확히 concurrentRequests만큼 증가했는지 확인
		UrlMapping updated = urlMappingRepository.findById(saved.getId())
			.orElseThrow(() -> new AssertionError("엔티티를 찾을 수 없음"));

		System.out.println("업데이트된 클릭 횟수: " + updated.getClickCount());
		System.out.println("예상 클릭 횟수: " + (initialClickCount + concurrentRequests));

		assertEquals(initialClickCount + concurrentRequests, updated.getClickCount(),
			"동시 요청 시에도 클릭 횟수가 정확히 " + concurrentRequests + "번 증가해야 함");

		System.out.println("✅ 레이스 컨디션 없음 - 클릭 횟수 정확");
	}
}
