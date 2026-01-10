package luti.server.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import luti.server.web.dto.response.AuthCheckResponse;
import luti.server.web.mapper.AuthExtractor;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	@GetMapping("/me")
	public ResponseEntity<AuthCheckResponse> isAuthenticated(Authentication authentication) {
		Boolean isAuthenticated = AuthExtractor.isAuthenticated(authentication);
		AuthCheckResponse response = AuthCheckResponse.of(isAuthenticated);

		return ResponseEntity.ok(response);
	}
}
