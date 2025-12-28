package luti.server.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.client.KeyBlockManager;
import luti.server.service.Base62Encoder;
import luti.server.service.IdScrambler;
import luti.server.service.UrlService;

@Component
public class ApiFacade {

	private static final Logger log = LoggerFactory.getLogger(ApiFacade.class);

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
		log.info("URL 단축 요청: originalUrl={}", originalUrl);

		try {
			Long nextId = keyBlockManager.getNextId();
			log.debug("KGS ID 획득: kgsId={}", nextId);

			Long scrambledId = idScrambler.scramble(nextId);
			log.debug("ID 스크램블링 완료: kgsId={}, scrambledId={}", nextId, scrambledId);

			String encodedValue = base62Encoder.encode(scrambledId);
			log.debug("Base62 인코딩 완료: scrambledId={}, shortCode={}", scrambledId, encodedValue);

			String shortenedUrl =
				urlService.generateShortenedUrl(originalUrl, nextId, scrambledId, encodedValue);

			log.info("URL 단축 성공: shortCode={}, kgsId={}, shortenedUrl={}", encodedValue, nextId, shortenedUrl);
			return shortenedUrl;

		} catch (Exception e) {
			log.error("URL 단축 실패: originalUrl={}", originalUrl, e);
			throw e;
		}
	}

}
