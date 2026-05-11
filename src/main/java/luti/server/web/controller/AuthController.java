package luti.server.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import luti.server.infrastructure.security.JwtProvider;
import luti.server.infrastructure.security.RefreshTokenStore;
import luti.server.web.dto.AuthCheckResponse;
import luti.server.web.resolver.AuthExtractor;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final JwtProvider jwtProvider;
	private final RefreshTokenStore refreshTokenStore;

	@Value("${app.cookie.domain:}")
	private String cookieDomain;

	@Value("${app.cookie.secure:false}")
	private boolean cookieSecure;

	@Value("${app.cookie.same-site:Lax}")
	private String cookieSameSite;

	public AuthController(JwtProvider jwtProvider, RefreshTokenStore refreshTokenStore) {
		this.jwtProvider = jwtProvider;
		this.refreshTokenStore = refreshTokenStore;
	}

	@GetMapping("/me")
	public AuthCheckResponse isAuthenticated(Authentication authentication) {
		boolean isAuthenticated = AuthExtractor.isAuthenticated(authentication);
		Long memberId = AuthExtractor.extractMemberId(authentication);

		return AuthCheckResponse.of(isAuthenticated, memberId, null);
	}

	@PostMapping("/refresh")
	public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {

		String refreshToken = extractCookieValue(request, "refresh_token");
		if (refreshToken == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		String jti;
		String memberId;
		List<String> roles;

		try {
			jti = jwtProvider.extractJti(refreshToken);
			memberId = jwtProvider.extractSubject(refreshToken);
			roles = jwtProvider.extractRoles(refreshToken);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		if (refreshTokenStore.findMemberId(jti).isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		refreshTokenStore.delete(jti);

		String newAccessToken = jwtProvider.issueAccessToken(memberId, roles);
		String newRefreshToken = jwtProvider.issueRefreshToken(memberId);
		String newRefreshJti = jwtProvider.extractJti(newRefreshToken);
		refreshTokenStore.save(newRefreshJti, memberId, jwtProvider.getRefreshTtlSeconds());

		response.addHeader(HttpHeaders.SET_COOKIE,
						   buildCookie("access_token", newAccessToken, jwtProvider.getAccessTtlSeconds()).toString());
		response.addHeader(HttpHeaders.SET_COOKIE,
						   buildCookie("refresh_token", newRefreshToken, jwtProvider.getRefreshTtlSeconds()).toString());

		return ResponseEntity.ok().build();
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {

		String refreshToken = extractCookieValue(request, "refresh_token");
		if (refreshToken != null) {
			try {
				String jti = jwtProvider.extractJti(refreshToken);
				refreshTokenStore.delete(jti);
			} catch (Exception ignored) {
			}
		}

		response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie("access_token").toString());
		response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie("refresh_token").toString());

		return ResponseEntity.ok().build();
	}

	private String extractCookieValue(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) return null;
		for (Cookie c : cookies) {
			if (name.equals(c.getName())) return c.getValue();
		}
		return null;
	}

	private ResponseCookie buildCookie(String name, String value, long maxAge) {
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(cookieSecure)
			.path("/")
			.maxAge(maxAge)
			.sameSite(cookieSameSite);

		if (cookieDomain != null && !cookieDomain.isBlank()) {
			builder.domain(cookieDomain);
		}

		return builder.build();
	}

	private ResponseCookie deleteCookie(String name) {
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
			.httpOnly(true)
			.secure(cookieSecure)
			.path("/")
			.maxAge(0)
			.sameSite(cookieSameSite);

		if (cookieDomain != null && !cookieDomain.isBlank()) {
			builder.domain(cookieDomain);
		}

		return builder.build();
	}
}
