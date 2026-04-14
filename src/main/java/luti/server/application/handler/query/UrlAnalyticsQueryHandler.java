package luti.server.application.handler.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.handler.QueryHandler;
import luti.server.application.query.UrlAnalyticsQuery;
import luti.server.application.result.UrlAnalyticsResult;
import luti.server.domain.service.UrlAnalyticsService;
import luti.server.domain.service.dto.UrlAnalyticsInfo;

@Component
public class UrlAnalyticsQueryHandler implements QueryHandler<UrlAnalyticsQuery, UrlAnalyticsResult> {

	private static final Logger log = LoggerFactory.getLogger(UrlAnalyticsQueryHandler.class);

	private final UrlAnalyticsService urlAnalyticsService;

	public UrlAnalyticsQueryHandler(UrlAnalyticsService urlAnalyticsService) {
		this.urlAnalyticsService = urlAnalyticsService;
	}

	@Override
	public UrlAnalyticsResult execute(UrlAnalyticsQuery query) {

		log.info("URL 통계 조회 요청: urlId={}, memberId={}", query.getUrlId(), query.getMemberId());

		UrlAnalyticsInfo analyticsInfo = urlAnalyticsService.getAnalytics(query.getUrlId(), query.getMemberId());

		return UrlAnalyticsResult.from(analyticsInfo);
	}

	@Override
	public Class<UrlAnalyticsQuery> getSupportedQueryType() {
		return UrlAnalyticsQuery.class;
	}
}
