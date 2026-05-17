package luti.server.infrastructure.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import luti.server.infrastructure.security.OAuth2LoginSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

	public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
		this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
	}

	@Bean
	public BearerTokenResolver bearerTokenResolver() {
		return request -> {
			String uri = request.getRequestURI();
			if (uri.equals("/api/v1/auth/refresh") || uri.equals("/api/v1/auth/logout")) {
				return null;
			}
			Cookie[] cookies = request.getCookies();
			if (cookies == null) return null;
			for (Cookie cookie: cookies) {
				if ("access_token".equals(cookie.getName())) {
					String v = cookie.getValue();
					return (v == null || v.isBlank()) ? null : v;
				}
			}
			return null;
		};
	}

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

		// 인가 룰
		http
			.authorizeHttpRequests(auth -> auth
				// My URLs 관련 -> 인증 필요
				.requestMatchers("/api/v1/my-urls/**").authenticated()
				// 태그 관련 -> 인증 필요
				.requestMatchers("/api/v1/tags/**").authenticated()
				// /me -> 인증 필요 (401로 apiFetch의 refreshPromise 경로 타도록)
				.requestMatchers("/api/v1/auth/me").authenticated()

				.anyRequest().permitAll()
			);

		return http.build();
	}
}
