package luti.server.application.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.infrastructure.client.kgs.KeyBlockManager;
import luti.server.application.command.ShortenUrlCommand;
import luti.server.application.result.ShortenUrlResult;
import luti.server.domain.util.Base62Encoder;
import luti.server.domain.util.IdScrambler;
import luti.server.domain.service.UrlService;

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

	/**
	 * URL 단축
	 * - keyword가 없는 경우: auto 로직 (KGS + ID Scrambling + Base62)
	 * - keyword가 있는 경우: keyword 로직 (검증 + 저장)
	 */
	public ShortenUrlResult shortenUrl(ShortenUrlCommand command) {

		log.info("URL 단축 요청: originalUrl={}", command.getOriginalUrl());

		boolean hasKeyword = command.getKeyword() != null && !command.getKeyword().isBlank();

		String shortenedUrl;

		if (!hasKeyword) {
			Long nextId = keyBlockManager.getNextId();
			Long scrambledId = idScrambler.scramble(nextId);
			String encodedValue = base62Encoder.encode(scrambledId);
			shortenedUrl =
				urlService.generateShortenedUrl(command.getOriginalUrl(), nextId, scrambledId, encodedValue,
												command.getMemberId());

			log.info("URL 단축 성공 (auto): shortCode={}, kgsId={}, shortenedUrl={}", encodedValue, nextId, shortenedUrl);
		} else {
			shortenedUrl = urlService.generateShortenedUrlWithKeyword(command.getOriginalUrl(), command.getKeyword(),
																	  command.getMemberId());

			log.info("URL 단축 성공 (keyword): shortCode={}, shortenedUrl={}", command.getKeyword(), shortenedUrl);
		}

		return ShortenUrlResult.of(shortenedUrl);

	}

}
