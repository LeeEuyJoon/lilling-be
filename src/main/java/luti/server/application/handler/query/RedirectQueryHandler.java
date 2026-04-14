package luti.server.application.handler.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.handler.QueryHandler;
import luti.server.application.query.RedirectQuery;
import luti.server.application.result.RedirectResult;
import luti.server.domain.service.ClickCountService;
import luti.server.domain.service.UrlQueryService;
import luti.server.domain.util.Base62Encoder;

@Component
public class RedirectQueryHandler implements QueryHandler<RedirectQuery, RedirectResult> {

	private static final Logger log = LoggerFactory.getLogger(RedirectQueryHandler.class);

	private final Base62Encoder base62Encoder;
	private final UrlQueryService urlQueryService;
	private final ClickCountService clickCountService;

	public RedirectQueryHandler(Base62Encoder base62Encoder, UrlQueryService urlQueryService,
								ClickCountService clickCountService) {
		this.base62Encoder = base62Encoder;
		this.urlQueryService = urlQueryService;
		this.clickCountService = clickCountService;
	}

	@Override
	public RedirectResult execute(RedirectQuery query) {
		log.info("리다이렉트 요청: shortCode={}", query.getShortCode());

		Long decodedId = base62Encoder.decode(query.getShortCode());
		clickCountService.recordClick(decodedId); // async

		String originalUrl = urlQueryService.getOriginalUrl(decodedId);
		RedirectResult result = RedirectResult.of(originalUrl);

		return result;
	}

	@Override
	public Class<RedirectQuery> getSupportedQueryType() {
		return RedirectQuery.class;
	}
}
