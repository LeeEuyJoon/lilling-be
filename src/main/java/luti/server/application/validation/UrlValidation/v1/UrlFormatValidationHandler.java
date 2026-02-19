package luti.server.application.validation.UrlValidation.v1;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import luti.server.application.result.UrlVerifyResult;
import luti.server.domain.service.UrlService;

public class UrlFormatValidationHandler implements UrlValidationHandler {

	private static final Logger log = LoggerFactory.getLogger(UrlFormatValidationHandler.class);

	private final UrlService urlService;
	private UrlValidationHandler next;

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

		if (next != null) {
			return next.validate(context);
		}

		return UrlVerifyResult.ok(context.getUrlMappingInfo());
	}

	@Override
	public void setNext(UrlValidationHandler next) {
		this.next = next;
	}
}
