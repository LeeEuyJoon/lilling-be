package luti.server.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import luti.server.facade.UrlShorteningFacade;
import luti.server.web.dto.ShortenRequest;
import luti.server.web.dto.ShortenResponse;

@RestController
@RequestMapping("/api/v1/url")
public class UrlShorteningController {

	private final UrlShorteningFacade urlShorteningFacade;

	public UrlShorteningController(UrlShorteningFacade urlShorteningFacade) {
		this.urlShorteningFacade = urlShorteningFacade;
	}

	@PostMapping("/shorten")
	public ResponseEntity<ShortenResponse> shortenUrl(@RequestBody ShortenRequest request, Authentication authentication) {

		String shortUrl = urlShorteningFacade.shortenUrl(request.getOriginalUrl(), authentication);
		return ResponseEntity.ok(ShortenResponse.of(shortUrl));
	}
}
