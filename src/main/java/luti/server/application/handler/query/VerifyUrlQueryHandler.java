package luti.server.application.handler.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.handler.QueryHandler;
import luti.server.application.query.VerifyUrlQuery;
import luti.server.application.result.UrlVerifyResult;
import luti.server.application.validation.UrlValidation.v2.UrlValidationChainBuilder;
import luti.server.application.validation.UrlValidation.v2.UrlValidationContext;
import luti.server.application.validation.UrlValidation.v2.UrlValidator;

@Component
public class VerifyUrlQueryHandler implements QueryHandler<VerifyUrlQuery, UrlVerifyResult> {

	private static final Logger log = LoggerFactory.getLogger(VerifyUrlQueryHandler.class);

	private final UrlValidationChainBuilder chainBuilder;

	public VerifyUrlQueryHandler(UrlValidationChainBuilder chainBuilder) {
		this.chainBuilder = chainBuilder;
	}

	@Override
	public UrlVerifyResult execute(VerifyUrlQuery query) {

		log.info("단축 URL 추가 가능 검증 요청: shortUrl={}", query.getShortUrl());

		UrlValidationContext context = new UrlValidationContext(query.getShortUrl());
		UrlValidator chain = chainBuilder.buildVerifyChain();

		return chain.validate(context);
	}

	@Override
	public Class<VerifyUrlQuery> getSupportedQueryType() {
		return VerifyUrlQuery.class;
	}
}
