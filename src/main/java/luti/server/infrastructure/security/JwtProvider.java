package luti.server.infrastructure.security;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtProvider {

	private final JwtEncoder jwtEncoder;

	@Value("${JWT_ISSUER}")
	private String issuer;

	@Value("${JWT_AUDIENCE}")
	private String audience;

	@Value("${JWT_ACCESS_TTL_SECONDS:3600}")
	private long accessTtlSeconds;

	public JwtProvider(JwtEncoder jwtEncoder) {
		this.jwtEncoder = jwtEncoder;
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
}
