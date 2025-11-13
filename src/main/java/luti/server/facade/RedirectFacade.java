package luti.server.facade;

import org.springframework.stereotype.Component;

import luti.server.service.Base62Encoder;
import luti.server.service.UrlService;

@Component
public class RedirectFacade {

	private final Base62Encoder base62Encoder;
	private final UrlService urlService;

	public RedirectFacade(Base62Encoder base62Encoder, UrlService urlService) {
		this.base62Encoder = base62Encoder;
		this.urlService = urlService;
	}

	public String getOriginalUrl(String shortCode) {

		Long decodedId = base62Encoder.decode(shortCode);
		String originalUrl = urlService.getOriginalUrl(decodedId);

		return originalUrl;
	}
}
