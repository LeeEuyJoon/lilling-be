package luti.server.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.service.Base62Encoder;
import luti.server.service.UrlService;

@Component
public class RedirectFacade {

	private static final Logger log = LoggerFactory.getLogger(RedirectFacade.class);

	private final Base62Encoder base62Encoder;
	private final UrlService urlService;

	public RedirectFacade(Base62Encoder base62Encoder, UrlService urlService) {
		this.base62Encoder = base62Encoder;
		this.urlService = urlService;
	}

	public String getOriginalUrl(String shortCode) {
		log.info("리다이렉트 요청: shortCode={}", shortCode);

		try {
			Long decodedId = base62Encoder.decode(shortCode);
			log.debug("Base62 디코딩 완료: shortCode={}, scrambledId={}", shortCode, decodedId);

			String originalUrl = urlService.getOriginalUrl(decodedId);

			if (originalUrl != null) {
				log.info("리다이렉트 성공: shortCode={}, originalUrl={}", shortCode, originalUrl);
			} else {
				log.warn("존재하지 않는 shortCode: {}", shortCode);
			}

			return originalUrl;

		} catch (Exception e) {
			log.error("리다이렉트 실패: shortCode={}", shortCode, e);
			throw e;
		}
	}
}
