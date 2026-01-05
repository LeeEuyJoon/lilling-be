package luti.server.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@SpringBootTest
class JwtServiceTest {

	@Autowired
	JwtService jwtService;

	@Autowired
	JwtDecoder jwtDecoder;

	@Test
	@DisplayName("액세스 토큰 발급 테스트")
	void issueAccessToken() {

		String userId = "123";
		List<String> roles = List.of("USER");

		String token = jwtService.issueAccessToken(userId, roles);

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