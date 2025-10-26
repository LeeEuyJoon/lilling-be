package luti.server.Web.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import luti.server.Facade.ApiFacade;
import luti.server.Web.Dto.ShortenRequest;
import luti.server.Web.Dto.ShortenResponse;

@RestController
@RequestMapping("/api/v1/url")
public class ApiController {

	private final ApiFacade apiFacade;

	public ApiController(ApiFacade apiFacade) {
		this.apiFacade = apiFacade;
	}

	@PostMapping("/shorten")
	public ResponseEntity<ShortenResponse> shortenUrl(@RequestBody @Valid ShortenRequest request) {

		String shortUrl = apiFacade.shortenUrl(request.getOriginalUrl());
		return ResponseEntity.ok(ShortenResponse.of(shortUrl));
	}
}
