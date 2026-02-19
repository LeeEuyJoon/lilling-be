package luti.server.application.validation.UrlValidation.v2;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.result.UrlVerifyResult;
import luti.server.domain.service.UrlService;
import luti.server.domain.service.dto.UrlMappingInfo;
import luti.server.domain.util.Base62Encoder;

@Component
public class UrlExistenceValidationHandler implements UrlValidationHandler {

	private static final Logger log = LoggerFactory.getLogger(UrlExistenceValidationHandler.class);

	private final Base62Encoder base62Encoder;
	private final UrlService urlService;

	public UrlExistenceValidationHandler(Base62Encoder base62Encoder, UrlService urlService) {
		this.base62Encoder = base62Encoder;
		this.urlService = urlService;
	}

	@Override
	public UrlVerifyResult validate(UrlValidationContext context) {
		log.debug("URL 존재 여부 검증 시작: shortCode={}", context.getShortCode());

		Long decodedId = base62Encoder.decode(context.getShortCode());
		context.setDecodedId(decodedId);

		Optional<UrlMappingInfo> urlInfo = urlService.findByDecodedId(decodedId);

		if (urlInfo.isEmpty()) {
			log.debug("URL 존재 여부 검증 실패: decodedId={}", decodedId);
			return UrlVerifyResult.notFound();
		}

		context.setUrlMappingInfo(urlInfo.get());
		log.debug("URL 존재 여부 검증 성공: decodedId={}", decodedId);

		return null;
	}
}
