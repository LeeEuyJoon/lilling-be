package luti.server.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;
import luti.server.infrastructure.security.AuthTokenService;
import luti.server.infrastructure.security.dto.TokenPair;
import luti.server.web.dto.AuthCheckResponse;
import luti.server.web.resolver.AuthExtractor;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthTokenService authTokenService;

	@Value("${app.cookie.domain:}")
	private String cookieDomain;

	@Value("${app.cookie.secure:false}")
	private boolean cookieSecure;

	@Value("${app.cookie.same-site:Lax}")
	private String cookieSameSite;

	public AuthController(AuthTokenService authTokenService) {
		this.authTokenService = authTokenService;
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
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		TokenPair tokenPair = authTokenService.rotate(refreshToken);

		response.addHeader(HttpHeaders.SET_COOKIE,
						   buildCookie("access_token", tokenPair.accessToken(), tokenPair.accessTtlSeconds()).toString());
		response.addHeader(HttpHeaders.SET_COOKIE,
						   buildCookie("refresh_token", tokenPair.refreshToken(), tokenPair.refreshTtlSeconds()).toString());

		return ResponseEntity.ok().build();
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = extractCookieValue(request, "refresh_token");
		if (refreshToken != null) {
			authTokenService.revoke(refreshToken);
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
