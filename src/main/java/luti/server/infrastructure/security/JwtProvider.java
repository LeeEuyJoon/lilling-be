package luti.server.infrastructure.security;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtProvider {

	private final JwtEncoder jwtEncoder;
	private final JwtDecoder jwtDecoder;

	@Value("${JWT_ISSUER}")
	private String issuer;

	@Value("${JWT_AUDIENCE}")
	private String audience;

	@Value("${JWT_ACCESS_TTL_SECONDS:3600}")
	private long accessTtlSeconds;

	@Value("${JWT_REFRESH_TTL_SECONDS:1209600}")
	private long refreshTtlSeconds;

	public JwtProvider(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder) {
		this.jwtEncoder = jwtEncoder;
		this.jwtDecoder = jwtDecoder;
	}

	public String issueAccessToken(String userId, Collection<String> roles) {
		Instant now = Instant.now();
		Instant expiredAt = now.plusSeconds(accessTtlSeconds);

		JwtClaimsSet claims = JwtClaimsSet.builder()
										  .issuer(issuer)
										  .subject(userId)
										  .audience(List.of(audience))
										  .issuedAt(now)
										  .expiresAt(expiredAt)
										  .id(UUID.randomUUID().toString())
										  .claim("roles", roles)
										  .build();

		return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}

	public String issueRefreshToken(String userId) {
		Instant now = Instant.now();

		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(issuer)
			.subject(userId)
			.audience(List.of(audience))
			.expiresAt(now.plusSeconds(refreshTtlSeconds))
			.id(UUID.randomUUID().toString())
			.issuedAt(now)
			.build();

		return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
	}

	public long getRefreshTtlSeconds() {
		return refreshTtlSeconds;
	}

	public long getAccessTtlSeconds() {
		return accessTtlSeconds;
	}

	// Refresh Token의 jti 추출 (Redis 키로 사용)
	public String extractJti(String token) {
		return jwtDecoder.decode(token).getId();
	}

	// Refresh Token의 subject (memberId) 추출
	public String extractSubject(String token) {
		return jwtDecoder.decode(token).getSubject();
	}

	// Refresh Token의 roles 추출
	public List<String> extractRoles(String token) {
		Jwt decoded = jwtDecoder.decode(token);
		Object roles = decoded.getClaim("roles");
		if (roles instanceof List<?> list) {
			return list.stream().map(Object::toString).toList();
		}
		return List.of();
	}
}

