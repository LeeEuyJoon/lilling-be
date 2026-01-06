package luti.server.security;

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
import luti.server.enums.Provider;
import luti.server.service.JwtService;
import luti.server.service.MemberProvisionService;
import luti.server.service.dto.ProvisionedMemberDto;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final JwtService jwtService;
	private final MemberProvisionService memberProvisionService;

	@Value("${app.frontend.redirect-url:http://localhost:3000/}")
	private String redirectUrl;

	@Value("${app.cookie.domain:}")
	private String cookieDomain; // 운영: .lill.ing / 로컬: 빈값

	@Value("${app.cookie.secure:false}")
	private boolean cookieSecure;

	@Value("${app.cookie.same-site:Lax}")
	private String cookieSameSite; // 운영 cross-site면 None 권장

	@Value("${JWT_ACCESS_TTL_SECONDS:3600}")
	private long accessTtlSeconds;

	public OAuth2LoginSuccessHandler(JwtService jwtService, MemberProvisionService memberProvisionService) {
		this.jwtService = jwtService;
		this.memberProvisionService = memberProvisionService;
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
			sub = oidcUser.getSubject(); // ✅ OIDC 표준 sub
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

		// DB에서 Member 확보
		ProvisionedMemberDto pm = memberProvisionService.findOrCreate(Provider.GOOGLE, sub, email);

		// Access JWT 발급 (sub = memberId)
		String accessToken = jwtService.issueAccessToken(
			pm.getMemberId().toString(),
			List.of(pm.getRole().name())
		);

		// 쿠키 세팅
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("access_token", accessToken)
																	 .httpOnly(true)
																	 .secure(cookieSecure)
																	 .path("/")
																	 .maxAge(accessTtlSeconds)
																	 .sameSite(cookieSameSite);

		// Domain은 로컬에선 비우고, 운영에서만 ".lill.ing"
		if (cookieDomain != null && !cookieDomain.isBlank()) {
			builder.domain(cookieDomain);
		}

		ResponseCookie cookie = builder.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

		// 프론트로 redirect
		response.sendRedirect(redirectUrl);
	}
}
