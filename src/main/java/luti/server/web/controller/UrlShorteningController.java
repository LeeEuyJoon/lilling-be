package luti.server.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import luti.server.application.facade.UrlShorteningFacade;
import luti.server.application.command.ShortenUrlCommand;
import luti.server.application.result.ShortenUrlResult;
import luti.server.web.dto.request.ShortenRequest;
import luti.server.web.dto.response.ShortenResponse;
import luti.server.web.mapper.ShortenUrlCommandMapper;

@RestController
@RequestMapping("/api/v1/url")
public class UrlShorteningController {

	private final UrlShorteningFacade urlShorteningFacade;

	public UrlShorteningController(UrlShorteningFacade urlShorteningFacade) {
		this.urlShorteningFacade = urlShorteningFacade;
	}

	@PostMapping("/shorten")
	public ResponseEntity<ShortenResponse> shortenUrl(@RequestBody ShortenRequest request, Authentication authentication) {

		ShortenUrlCommand command = ShortenUrlCommandMapper.toCommand(request, authentication);
		ShortenUrlResult result = urlShorteningFacade.shortenUrl(command);

		return ResponseEntity.ok(ShortenResponse.from(result));
	}
}
