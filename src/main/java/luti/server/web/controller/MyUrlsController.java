package luti.server.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import luti.server.enums.VerifyUrlStatus;
import luti.server.facade.MyUrlsFacade;
import luti.server.facade.dto.UrlVerifyResult;
import luti.server.web.dto.VerifyUrlResponse;

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
}
