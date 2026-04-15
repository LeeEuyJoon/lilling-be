package luti.server.web.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import luti.server.web.dto.AuthCheckResponse;
import luti.server.web.resolver.AuthExtractor;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	@GetMapping("/me")
	public AuthCheckResponse isAuthenticated(Authentication authentication) {
		boolean isAuthenticated = AuthExtractor.isAuthenticated(authentication);
		Long memberId = AuthExtractor.extractMemberId(authentication);

		return AuthCheckResponse.of(isAuthenticated, memberId, null);
	}
}
