package luti.server.Web.Controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import luti.server.Service.UrlService;

@RestController
@RequestMapping("/")
public class RedirectController {

	private final UrlService urlService;

	public RedirectController(UrlService urlService) {
		this.urlService = urlService;
	}

	@GetMapping("/{shortCode}")
	public ResponseEntity<Void>
	redirect(@PathVariable String shortCode) {

		String originalUrl = urlService.getOriginalUrl(shortCode);

		return ResponseEntity
			.status(HttpStatus.MOVED_PERMANENTLY)
			.location(URI.create(originalUrl))
			.build();
	}

}
