package luti.server.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.client.KeyBlockManager;
import luti.server.facade.command.ShortenUrlCommand;
import luti.server.facade.result.ShortenUrlResult;
import luti.server.util.Base62Encoder;
import luti.server.util.IdScrambler;
import luti.server.service.UrlService;

@Component
public class UrlShorteningFacade {

	private static final Logger log = LoggerFactory.getLogger(UrlShorteningFacade.class);

	private final IdScrambler idScrambler;
	private final Base62Encoder base62Encoder;
	private final KeyBlockManager keyBlockManager;
	private final UrlService urlService;

	public UrlShorteningFacade(IdScrambler idScrambler, Base62Encoder base62Encoder, KeyBlockManager keyBlockManager,
							   UrlService urlService) {
		this.idScrambler = idScrambler;
		this.base62Encoder = base62Encoder;
		this.keyBlockManager = keyBlockManager;
		this.urlService = urlService;
	}

	public ShortenUrlResult shortenUrl(ShortenUrlCommand command) {
		log.info("URL 단축 요청: originalUrl={}", command.getOriginalUrl());

		Long nextId = keyBlockManager.getNextId();
		Long scrambledId = idScrambler.scramble(nextId);
		String encodedValue = base62Encoder.encode(scrambledId);
		String shortenedUrl =
			urlService.generateShortenedUrl(command.getOriginalUrl(), nextId, scrambledId, encodedValue, command.getMemberId());

		log.info("URL 단축 성공: shortCode={}, kgsId={}, shortenedUrl={}", encodedValue, nextId, shortenedUrl);
		return ShortenUrlResult.of(shortenedUrl);

	}

}
