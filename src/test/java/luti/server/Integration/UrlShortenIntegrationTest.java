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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import luti.server.Client.KgsClient;
import luti.server.Client.dto.KeyBlock;
import luti.server.Entity.UrlMapping;
import luti.server.Repository.UrlMappingRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UrlShortenIntegrationTest {

	@Container
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
		.withDatabaseName("testdb")
		.withUsername("test")
		.withPassword("test");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
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
		assertNotNull(saved.getShortUrl());
		assertNotNull(saved.getScrambledId());
		assertNotNull(saved.getCreatedAt());

		System.out.println("=== URL 단축 결과 ===");
		System.out.println("원본 URL: " + saved.getOriginalUrl());
		System.out.println("단축 URL: " + saved.getShortUrl());
		System.out.println("Scrambled ID: " + saved.getScrambledId());
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
		long id1 = urls.get(0).getScrambledId();
		long id2 = urls.get(1).getScrambledId();
		long id3 = urls.get(2).getScrambledId();

		// 모두 다른 scrambledId를 가져야 함
		assertNotEquals(id1, id2);
		assertNotEquals(id2, id3);
		assertNotEquals(id1, id3);

		System.out.println("=== 고유 ID 검증 ===");
		System.out.println("URL1 scrambledId: " + id1);
		System.out.println("URL2 scrambledId: " + id2);
		System.out.println("URL3 scrambledId: " + id3);
	}

	@Test
	@DisplayName("shortCode가 7자 이하인지 검증 (Base62)")
	void shortenUrl_shortCode길이_검증() throws Exception {
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
			.andExpect(status().isMovedPermanently())
			.andExpect(header().string("Location", originalUrl));

		System.out.println("=== 리다이렉트 검증 ===");
		System.out.println("ShortCode: " + shortCode);
		System.out.println("리다이렉트 대상: " + originalUrl);
	}

}
