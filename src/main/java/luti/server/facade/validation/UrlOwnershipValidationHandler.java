package luti.server.facade.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.facade.result.UrlVerifyResult;
import luti.server.service.dto.UrlMappingInfo;

@Component
public class UrlOwnershipValidationHandler implements UrlValidationHandler {

	private static final Logger log = LoggerFactory.getLogger(UrlOwnershipValidationHandler.class);

	private UrlValidationHandler next;

	@Override
	public UrlVerifyResult validate(UrlValidationContext context) {
		log.debug("소유권 검증 시작");

		UrlMappingInfo urlInfo = context.getUrlMappingInfo();

		if (urlInfo.isHasOwner()) {
			log.debug("소유권 검증 실패: 이미 소유자가 있음");
			return UrlVerifyResult.alreadyOwned();
		}

		log.debug("소유권 검증 성공: 소유자 없음");

		if (next != null) {
			return next.validate(context);
		}

		return UrlVerifyResult.ok(urlInfo);
	}

	@Override
	public void setNext(UrlValidationHandler next) {
		this.next = next;
	}
}
