package luti.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class LillingApplicationTests {

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
		registry.add("DOMAIN", () -> "https://lill.ing");
		registry.add("SCRAMBLING_CONST_XOR1", () -> "13");
		registry.add("SCRAMBLING_CONST_XOR2", () -> "7");
		registry.add("SCRAMBLING_CONST_XOR3", () -> "17");
	}

	@Test
	void contextLoads() {
	}

}
