package luti.server.infrastructure.config;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Configuration
public class JwtConfig {

	@Value("${JWT_PRIVATE_KEY_B64}")
	private String privateKeyB64;

	@Value("${JWT_PUBLIC_KEY_B64}")
	private String publicKeyB64;

	/**
	 * RS256 서명용 JwtEncoder (private key 사용)
	 */
	@Bean
	public JwtEncoder jwtEncoder() {
		RSAKey rsaKey = new RSAKey.Builder(getPublicKey())
								.privateKey(getPrivateKey())
								.keyID(UUID.randomUUID().toString()) // kid
								.build();

		JWKSet jwkSet = new JWKSet(rsaKey);

		return new NimbusJwtEncoder(new ImmutableJWKSet< >(jwkSet));
	}

	/**
	 * RS256 검증용 JwtDecoder (public key 사용)
	 */
	@Bean
	public JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withPublicKey(getPublicKey()).build();
	}

    /* =======================
       Key 변환 로직
       ======================= */

	private RSAPrivateKey getPrivateKey() {
		try {
			byte[] keyBytes = Base64.getDecoder().decode(privateKeyB64);
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return (RSAPrivateKey)keyFactory.generatePrivate(spec);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load RSA private key", e);
		}
	}

	private RSAPublicKey getPublicKey() {
		try {
			byte[] keyBytes = Base64.getDecoder().decode(publicKeyB64);
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return (RSAPublicKey)keyFactory.generatePublic(spec);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load RSA public key", e);
		}
	}
}
