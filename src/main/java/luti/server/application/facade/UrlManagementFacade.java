package luti.server.application.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.command.ClaimUrlCommand;
import luti.server.application.command.DeleteUrlCommand;
import luti.server.application.command.DescriptionCommand;
import luti.server.application.result.UrlVerifyResult;
import luti.server.application.validation.UrlValidation.v2.UrlValidationChainBuilder;
import luti.server.application.validation.UrlValidation.v2.UrlValidationContext;
import luti.server.application.validation.UrlValidation.v2.UrlValidator;
import luti.server.domain.service.MyUrlService;
import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;

@Component
public class UrlManagementFacade {

	private static final Logger log = LoggerFactory.getLogger(UrlManagementFacade.class);

	private final UrlValidationChainBuilder chainBuilder;
	private final MyUrlService myUrlService;

	public UrlManagementFacade(UrlValidationChainBuilder chainBuilder, MyUrlService myUrlService) {
		this.chainBuilder = chainBuilder;
		this.myUrlService = myUrlService;
	}

	/**
	 * URL 검증
	 */
	public UrlVerifyResult verify(String shortUrl) {

		log.info("단축 URL 추가 가능 검증 요청: shortUrl={}", shortUrl);

		UrlValidationContext context = new UrlValidationContext(shortUrl);
		UrlValidator chain = chainBuilder.buildVerifyChain();

		return chain.validate(context);
	}

	/**
	 * URL 클레임
	 */
	public void claimUrl(ClaimUrlCommand command) {

		log.info("단축 URL 클레임 요청: shortUrl={}, memberId={}", command.getShortUrl(), command.getMemberId());

		UrlValidationContext context = new UrlValidationContext(command.getShortUrl());
		UrlValidator chain = chainBuilder.buildClaimChain();

		UrlVerifyResult result = chain.validate(context);

		if (!result.isOk()) {
			throw new BusinessException(ErrorCode.INVALID_SHORT_URL_FORMAT);
		}

		myUrlService.claimUrlMappingToMember(context.getUrlMappingInfo(), command.getMemberId());
	}

	/**
	 * URL 설명 수정
	 */
	public void updateDescription(DescriptionCommand command) {

		log.info("단축 URL Description 수정 요청: memberId={}, urlId={}, description={}",
				 command.getMemberId(), command.getUrlId(), command.getDescription());

		myUrlService.updateUrlDescription(command.getUrlId(), command.getMemberId(), command.getDescription());
	}

	/**
	 * URL 삭제
	 */
	public void deleteUrl(DeleteUrlCommand command) {

		log.info("단축 URL 삭제 요청: memberId={}, urlId={}", command.getMemberId(), command.getUrlId());

		myUrlService.deleteUrlMapping(command.getUrlId(), command.getMemberId());
	}


}
