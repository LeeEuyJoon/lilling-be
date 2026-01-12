package luti.server.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;
import luti.server.facade.command.ClaimUrlCommand;
import luti.server.facade.command.MyUrlsCommand;
import luti.server.facade.result.MyUrlsListResult;
import luti.server.facade.result.UrlVerifyResult;
import luti.server.facade.validation.UrlValidationChainBuilder;
import luti.server.facade.validation.UrlValidationContext;
import luti.server.facade.validation.UrlValidationHandler;
import luti.server.service.MyUrlService;
import luti.server.service.dto.MyUrlsListInfo;

@Component
public class MyUrlsFacade {

	private static final Logger log = LoggerFactory.getLogger(MyUrlsFacade.class);

	private final UrlValidationChainBuilder chainBuilder;
	private final MyUrlService myUrlService;

	public MyUrlsFacade(UrlValidationChainBuilder chainBuilder, MyUrlService myUrlService) {
		this.chainBuilder = chainBuilder;
		this.myUrlService = myUrlService;
	}

	public UrlVerifyResult verify(String shortUrl) {
		log.info("단축 URL 추가 가능 검증 요청: shortUrl={}", shortUrl);

		UrlValidationContext context = new UrlValidationContext(shortUrl);
		UrlValidationHandler chain = chainBuilder.buildVerifyChain();

		return chain.validate(context);
	}

	public void claimUrl(ClaimUrlCommand command) {
		log.info("단축 URL 클레임 요청: shortUrl={}, memberId={}", command.getShortUrl(), command.getMemberId());

		UrlValidationContext context = new UrlValidationContext(command.getShortUrl());
		UrlValidationHandler chain = chainBuilder.buildClaimChain();

		UrlVerifyResult result = chain.validate(context);

		if (!result.isOk()) {
			throw new BusinessException(ErrorCode.INVALID_SHORT_URL_FORMAT);
		}

		myUrlService.claimUrlMappingToMember(context.getUrlMappingInfo(), command.getMemberId());
	}

	public MyUrlsListResult getMyUrls(MyUrlsCommand command) {

		log.info("단축 URL 목록 조회 요청: memberId={}, page={}, size={}",
				command.getMemberId(), command.getPage(), command.getSize());

		MyUrlsListInfo urlsListInfo = myUrlService.getMyUrls(command.getMemberId(), command.getPage(), command.getSize());

		return MyUrlsListResult.from(urlsListInfo);

	}
}
