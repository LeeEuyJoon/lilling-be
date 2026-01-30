package luti.server.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import luti.server.application.command.UrlAnalyticsCommand;
import luti.server.application.facade.MyUrlsFacade;
import luti.server.application.command.ClaimUrlCommand;
import luti.server.application.command.DeleteUrlCommand;
import luti.server.application.command.DescriptionCommand;
import luti.server.application.command.MyUrlsCommand;
import luti.server.application.result.MyUrlsListResult;
import luti.server.application.result.UrlAnalyticsResult;
import luti.server.application.result.UrlVerifyResult;
import luti.server.web.dto.request.DescriptionRequest;
import luti.server.web.dto.response.UrlAnalyticsResponse;
import luti.server.web.mapper.DeleteUrlCommandMapper;
import luti.server.web.mapper.DescriptionCommandMapper;
import luti.server.web.mapper.MyUrlsCommandMapper;
import luti.server.web.dto.request.ClaimRequest;
import luti.server.web.dto.response.MyUrlsListResponse;
import luti.server.web.dto.response.VerifyUrlResponse;
import luti.server.web.mapper.ClaimUrlCommandMapper;
import luti.server.web.mapper.UrlAnalyticsCommandMapper;

@RestController
@RequestMapping("/api/v1/my-urls")
public class MyUrlsController {

	private final MyUrlsFacade myUrlsFacade;

	public MyUrlsController(MyUrlsFacade myUrlsFacade) {
		this.myUrlsFacade = myUrlsFacade;
	}

	@GetMapping("/verify")
	public ResponseEntity<VerifyUrlResponse> verify(@RequestParam("shortUrl") String shortUrl) {

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

	@GetMapping("/list")
	public ResponseEntity<MyUrlsListResponse> getMyUrls(@RequestParam(value = "page", defaultValue = "0") Integer page,
														@RequestParam(value = "size", defaultValue = "10") Integer size,
														Authentication authentication) {

		MyUrlsCommand command = MyUrlsCommandMapper.toCommand(page, size, authentication);
		MyUrlsListResult result = myUrlsFacade.getMyUrls(command);
		MyUrlsListResponse response = MyUrlsListResponse.from(result);

		return ResponseEntity.ok(response);
	}

	@PatchMapping("/description")
	public ResponseEntity<Void> updateDescription(@RequestBody DescriptionRequest request,
												  Authentication authentication) {

		DescriptionCommand command = DescriptionCommandMapper.toCommand(request, authentication);
		myUrlsFacade.updateDescription(command);

		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{urlId}")
	public ResponseEntity<Void> deleteUrl(@PathVariable("urlId") Long urlId, Authentication authentication) {

		DeleteUrlCommand command = DeleteUrlCommandMapper.toCommand(urlId, authentication);
		myUrlsFacade.deleteUrl(command);

		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{urlId}/analytics")
	public ResponseEntity<UrlAnalyticsResponse> getUrlAnalytics(@PathVariable("urlId") Long urlId, Authentication authentication) {

		UrlAnalyticsCommand command = UrlAnalyticsCommandMapper.toCommand(urlId, authentication);
		UrlAnalyticsResult result = myUrlsFacade.getUrlAnalytics(command);
		UrlAnalyticsResponse response = UrlAnalyticsResponse.from(result);

		return ResponseEntity.ok(response);
	}
}
