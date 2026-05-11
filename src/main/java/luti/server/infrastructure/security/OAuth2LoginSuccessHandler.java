package luti.server.infrastructure.security;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import luti.server.domain.enums.Provider;
import luti.server.infrastructure.security.dto.ProvisionedMemberDto;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final JwtProvider jwtProvider;
	private final OAuth2MemberProvisioner oAuth2MemberProvisioner;
	private final RefreshTokenStore refreshTokenStore;

	@Value("${app.frontend.redirect-url:http://localhost:3000/}")
	private String redirectUrl;

	@Value("${app.cookie.domain:}")
	private String cookieDomain; // 운영: .lill.ing / 로컬: 빈값

	@Value("${app.cookie.secure:false}")
	private boolean cookieSecure;

	@Value("${app.cookie.same-site:Lax}")
	private String cookieSameSite; // 운영 cross-site면 None 권장

	public OAuth2LoginSuccessHandler(JwtProvider jwtProvider, OAuth2MemberProvisioner oAuth2MemberProvisioner,
									 RefreshTokenStore refreshTokenStore) {
		this.jwtProvider = jwtProvider;
		this.oAuth2MemberProvisioner = oAuth2MemberProvisioner;
		this.refreshTokenStore = refreshTokenStore;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request,
										HttpServletResponse response,
										Authentication authentication) throws IOException, ServletException {

		// 1) principal에서 sub/email 추출
		String sub = null;
		String email = null;

		Object principal = authentication.getPrincipal();

		if (principal instanceof OidcUser oidcUser) {
			sub = oidcUser.getSubject(); // OIDC 표준 sub
			email = oidcUser.getEmail(); // null 가능
		} else if (principal instanceof OAuth2User oAuth2User) {
			Object subAttr = oAuth2User.getAttributes().get("sub");
			if (subAttr != null) sub = subAttr.toString();
			Object emailAttr = oAuth2User.getAttributes().get("email");
			if (emailAttr != null) email = emailAttr.toString();
		}

		if (sub == null) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing subject(sub)");
			return;
		}

		ProvisionedMemberDto pm = oAuth2MemberProvisioner.findOrCreate(Provider.GOOGLE, sub, email);

		String accessToken = jwtProvider.issueAccessToken(
			pm.getMemberId().toString(),
			List.of(pm.getRole().name())
		);

		String refreshToken = jwtProvider.issueRefreshToken(pm.getMemberId().toString());
		String refreshJti = jwtProvider.extractJti(refreshToken);
		refreshTokenStore.save(refreshJti, pm.getMemberId().toString(), jwtProvider.getRefreshTtlSeconds());

		response.addHeader(HttpHeaders.SET_COOKIE,
						   buildCookie("access_token", accessToken, jwtProvider.getAccessTtlSeconds()).toString());
		response.addHeader(HttpHeaders.SET_COOKIE,
						   buildCookie("refresh_token", refreshToken, jwtProvider.getRefreshTtlSeconds()).toString());

		response.sendRedirect(redirectUrl);
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
}
