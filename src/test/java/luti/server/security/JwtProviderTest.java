package luti.server.security;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import luti.server.infrastructure.security.JwtProvider;

@SpringBootTest
@Testcontainers
class JwtProviderTest {

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
	}

	@Autowired
	JwtProvider jwtProvider;

	@Autowired
	JwtDecoder jwtDecoder;

	@Test
	@DisplayName("액세스 토큰 발급 테스트")
	void issueAccessToken() {

		String userId = "123";
		List<String> roles = List.of("USER");

		String token = jwtProvider.issueAccessToken(userId, roles);

		Jwt jwt = jwtDecoder.decode(token);

		assertThat(jwt.getIssuer().toString()).isEqualTo("https://api.lill.ing");
		assertThat(jwt.getSubject()).isEqualTo("123");
		assertThat(jwt.getAudience()).contains("api.lill.ing");
		assertThat(jwt.getClaimAsStringList("roles")).containsExactly("USER");
		assertThat(jwt.getExpiresAt()).isNotNull();
		assertThat(jwt.getIssuedAt()).isNotNull();

		System.out.println("발급된 액세스 토큰: " + token);
	}
}