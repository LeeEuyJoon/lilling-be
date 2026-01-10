package luti.server.web.mapper;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

public class AuthExtractor {
	public static Long extractMemberId(Authentication authentication) {
		if (authentication == null) {
			return null;
		}

		if (authentication instanceof AnonymousAuthenticationToken) {
			return null;
		}

		String name = authentication.getName();

		if (name == null || name.isBlank()) {
			return null;
		}

		try {
			return Long.parseLong(name);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static boolean isAuthenticated(Authentication authentication) {
		return authentication != null
			&& authentication.isAuthenticated()
			&& !(authentication instanceof AnonymousAuthenticationToken);
	}
}
