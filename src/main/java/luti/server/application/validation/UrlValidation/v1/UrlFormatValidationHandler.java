package luti.server.application.validation.UrlValidation.v1;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import luti.server.application.result.UrlVerifyResult;
import luti.server.domain.service.UrlQueryService;

public class UrlFormatValidationHandler implements UrlValidationHandler {

	private static final Logger log = LoggerFactory.getLogger(UrlFormatValidationHandler.class);

	private final UrlQueryService urlQueryService;
	private UrlValidationHandler next;

	public UrlFormatValidationHandler(UrlQueryService urlQueryService) {
		this.urlQueryService = urlQueryService;
	}

	@Override
	public UrlVerifyResult validate(UrlValidationContext context) {
		log.debug("URL 형식 검증 시작: url={}", context.getShortUrl());

		Optional<String> shortCode = urlQueryService.verifyAndExtractShortCode(context.getShortUrl());

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
