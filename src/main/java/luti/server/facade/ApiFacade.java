package luti.server.facade;

import org.springframework.stereotype.Component;

import luti.server.client.KeyBlockManager;
import luti.server.service.Base62Encoder;
import luti.server.service.IdScrambler;
import luti.server.service.UrlService;

@Component
public class ApiFacade {

	private final IdScrambler idScrambler;
	private final Base62Encoder base62Encoder;
	private final KeyBlockManager keyBlockManager;
	private final UrlService urlService;

	public ApiFacade(IdScrambler idScrambler, Base62Encoder base62Encoder, KeyBlockManager keyBlockManager,
		UrlService urlService) {
		this.idScrambler = idScrambler;
		this.base62Encoder = base62Encoder;
		this.keyBlockManager = keyBlockManager;
		this.urlService = urlService;
	}

	public String shortenUrl(String originalUrl) {

		Long nextId = keyBlockManager.getNextId();
		Long scrambledId = idScrambler.scramble(nextId);
		String encodedValue = base62Encoder.encode(scrambledId);
		String shortenedUrl =
			urlService.generateShortenedUrl(originalUrl, nextId, scrambledId, encodedValue);

		return shortenedUrl;
	}

}
