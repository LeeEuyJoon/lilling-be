package luti.server.application.handler.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.command.legacy.ClaimUrlCommand;
import luti.server.application.handler.CommandHandler;
import luti.server.application.result.UrlVerifyResult;
import luti.server.application.validation.UrlValidation.v2.UrlValidationChainBuilder;
import luti.server.application.validation.UrlValidation.v2.UrlValidationContext;
import luti.server.application.validation.UrlValidation.v2.UrlValidator;
import luti.server.domain.service.MyUrlService;
import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;

@Component
public class ClaimUrlCommandHandler implements CommandHandler<ClaimUrlCommand, Void> {

	private static final Logger log = LoggerFactory.getLogger(ClaimUrlCommandHandler.class);

	private final UrlValidationChainBuilder chainBuilder;
	private final MyUrlService myUrlService;

	public ClaimUrlCommandHandler(UrlValidationChainBuilder chainBuilder, MyUrlService myUrlService) {
		this.chainBuilder = chainBuilder;
		this.myUrlService = myUrlService;
	}

	@Override
	public Void execute(ClaimUrlCommand command) {

		log.info("단축 URL 클레임 요청: shortUrl={}, memberId={}", command.getShortUrl(), command.getMemberId());

		UrlValidationContext context = new UrlValidationContext(command.getShortUrl());
		UrlValidator chain = chainBuilder.buildClaimChain();

		UrlVerifyResult result = chain.validate(context);

		if (!result.isOk()) {
			throw new BusinessException(ErrorCode.INVALID_SHORT_URL_FORMAT);
		}

		myUrlService.claimUrlMappingToMember(context.getUrlMappingInfo(), command.getMemberId());

		return null;
	}

	@Override
	public Class<ClaimUrlCommand> getSupportedCommandType() {
		return ClaimUrlCommand.class;
	}
}
