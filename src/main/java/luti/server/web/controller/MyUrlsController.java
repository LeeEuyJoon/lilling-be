package luti.server.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import luti.server.facade.MyUrlsFacade;
import luti.server.facade.command.ClaimUrlCommand;
import luti.server.facade.result.UrlVerifyResult;
import luti.server.web.dto.request.ClaimRequest;
import luti.server.web.dto.response.VerifyUrlResponse;
import luti.server.web.mapper.ClaimUrlCommandMapper;

@RestController
@RequestMapping("/api/v1/my-urls")
public class MyUrlsController {

	private final MyUrlsFacade myUrlsFacade;

	public MyUrlsController(MyUrlsFacade myUrlsFacade) {
		this.myUrlsFacade = myUrlsFacade;
	}

	@GetMapping("/verify/{shortUrl}")
	public ResponseEntity<VerifyUrlResponse> verify(@PathVariable("shortUrl") String shortUrl) {

		UrlVerifyResult verifyResult = myUrlsFacade.verify(shortUrl);
		VerifyUrlResponse response = VerifyUrlResponse.from(verifyResult);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/claim")
	public ResponseEntity<Void> claimUrl(@RequestBody ClaimRequest request, Authentication authentication) {

		ClaimUrlCommand command = ClaimUrlCommandMapper.toCommand(request, authentication);
		myUrlsFacade.claimUrl(command);

		return ResponseEntity.noContent().build();
	}
}
