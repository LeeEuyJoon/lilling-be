package luti.server.application.validation.UrlValidation.v2;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.result.UrlVerifyResult;
import luti.server.domain.service.UrlService;

@Component
public class UrlFormatValidationHandler implements UrlValidationHandler {

	private static final Logger log = LoggerFactory.getLogger(UrlFormatValidationHandler.class);

	private final UrlService urlService;

	public UrlFormatValidationHandler(UrlService urlService) {
		this.urlService = urlService;
	}

	@Override
	public UrlVerifyResult validate(UrlValidationContext context) {
		log.debug("URL 형식 검증 시작: url={}", context.getShortUrl());

		Optional<String> shortCode = urlService.verifyAndExtractShortCode(context.getShortUrl());

		if (shortCode.isEmpty()) {
			log.debug("URL 형식 검증 실패: url={}", context.getShortUrl());
			return UrlVerifyResult.invalidFormat();
		}

		context.setShortCode(shortCode.get());
		log.debug("URL 형식 검증 성공: shortCode={}", shortCode.get());

		return null;
	}
}
