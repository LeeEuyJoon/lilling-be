package luti.server.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import luti.server.security.OAuth2LoginSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

	public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
		this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
	}

	/**
	 * BearerTokenResolver:
	 * - 기본은 Authorization: Bearer 토큰에서 꺼내지만
	 * - 우리는 HttpOnly 쿠키(access_token)에서 꺼낸다.
	 */
	@Bean
	public BearerTokenResolver bearerTokenResolver() {
		return new BearerTokenResolver() {
			@Override
			public String resolve(HttpServletRequest request) {
				Cookie[] cookies = request.getCookies();
				if (cookies == null) return null;

				for (Cookie c : cookies) {
					if ("access_token".equals(c.getName())) {
						String v = c.getValue();
						return (v == null || v.isBlank()) ? null : v;
					}
				}
				return null;
			}
		};
	}

	/**
	 * CORS:
	 * - allowCredentials(true) + allowedOriginPatterns 사용(운영/로컬 대응)
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource(
		@Value("${app.cors.allowed-origins}") String allowedOrigins
	) {
		List<String> origins = List.of(allowedOrigins.split("\\s*,\\s*"));

		return request -> {
			CorsConfiguration config = new CorsConfiguration();
			config.setAllowedOrigins(origins);
			config.setAllowCredentials(true);
			config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
			config.setAllowedHeaders(List.of("*"));
			config.setExposedHeaders(List.of("Set-Cookie"));
			config.setMaxAge(3600L);
			return config;
		};
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
												   BearerTokenResolver bearerTokenResolver) throws Exception {

		http
			// CORS
			.cors(cors -> cors.configurationSource(corsConfigurationSource(null)))
			// CSRF disable
			.csrf(csrf -> csrf.disable())
			// 세션 disable
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			// 폼 로그인 disable
			.formLogin(form -> form.disable())
			// HTTP Basic disable
			.httpBasic(basic -> basic.disable());

		// OAuth2 - 구글
		http
			.oauth2Login(oauth2 -> oauth2
				.successHandler(oAuth2LoginSuccessHandler)
			);

		// Resource Server (JWT 검증)
		http
			.oauth2ResourceServer(oauth2rs -> oauth2rs
				.bearerTokenResolver(bearerTokenResolver)
				.jwt(Customizer.withDefaults())
			);

		// 인가 룰(필요한 것만 딱 열어두기)
		http
			.authorizeHttpRequests(auth -> auth
				// 프리플라이트
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

				// 헬스체크/루트 등
				.requestMatchers("/", "/health").permitAll()

				// OAuth2 진입/콜백 경로 (스프링 기본)
				.requestMatchers("/oauth2/**", "/login/**").permitAll()

				// 로그인 상태 확인 API
				.requestMatchers("/api/v1/auth/me").permitAll()

				// URL 단축 API 비회원 가능
				.requestMatchers("/api/v1/url/shorten").permitAll()

				// 나머지는 인증 필요 (나머지가 마이페이지 기능밖에 없을 듯 당장은)
				.anyRequest().authenticated()
			);

		return http.build();
	}
}
