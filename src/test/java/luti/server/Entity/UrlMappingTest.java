package luti.server.Entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UrlMappingTest {

	@Test
	@DisplayName("UrlMapping 빌더 생성 테스트")
	void testUrlMappingCreation() {
		UrlMapping urlMapping = UrlMapping.builder().
			originalUrl("https://original-url.com").
			scrambleId(12345L).
			shortUrl("https://lilling/abcde").
			build();

		assertEquals("https://original-url.com", urlMapping.getOriginalUrl());
		assertEquals(12345L, urlMapping.getScrambleId());
		assertEquals("https://lilling/abcde", urlMapping.getShortUrl());

		System.out.println("originalUrl: " + urlMapping.getOriginalUrl());
		System.out.println("scrambleId: " + urlMapping.getScrambleId());
		System.out.println("shortUrl: " + urlMapping.getShortUrl());
		System.out.println("createdAt: " + urlMapping.getCreatedAt());

	}
}