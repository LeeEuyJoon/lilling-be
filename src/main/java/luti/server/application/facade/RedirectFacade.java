package luti.server.application.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.result.RedirectResult;
import luti.server.domain.util.Base62Encoder;
import luti.server.domain.service.ClickCountService;
import luti.server.domain.service.UrlService;

@Component
public class RedirectFacade {

	private static final Logger log = LoggerFactory.getLogger(RedirectFacade.class);

	private final Base62Encoder base62Encoder;
	private final UrlService urlService;
	private final ClickCountService clickCountService;

	public RedirectFacade(Base62Encoder base62Encoder, UrlService urlService, ClickCountService clickCountService) {
		this.base62Encoder = base62Encoder;
		this.urlService = urlService;
		this.clickCountService = clickCountService;
	}

	public RedirectResult getOriginalUrl(String shortCode) {
		log.info("리다이렉트 요청: shortCode={}", shortCode);

		Long decodedId = base62Encoder.decode(shortCode);
		clickCountService.recordClick(decodedId); // async

		String originalUrl = urlService.getOriginalUrl(decodedId);
		RedirectResult result = RedirectResult.of(originalUrl);

		return result;
	}
}
