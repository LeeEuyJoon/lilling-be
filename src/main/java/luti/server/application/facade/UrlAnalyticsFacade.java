package luti.server.application.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import luti.server.application.command.MyUrlsCommand;
import luti.server.application.command.UrlAnalyticsCommand;
import luti.server.application.result.MyUrlsListResult;
import luti.server.application.result.UrlAnalyticsResult;
import luti.server.domain.service.ClickStatisticsService;
import luti.server.domain.service.MyUrlService;
import luti.server.domain.service.TagService;
import luti.server.domain.service.UrlAnalyticsService;
import luti.server.domain.service.dto.MyUrlsListInfo;
import luti.server.domain.service.dto.RecentDailyStatisticsInfo;
import luti.server.domain.service.dto.TagInfo;
import luti.server.domain.service.dto.UrlAnalyticsInfo;

@Component
public class UrlAnalyticsFacade {

	private final Logger log = LoggerFactory.getLogger(UrlAnalyticsFacade.class);

	private final MyUrlService myUrlService;
	private final ClickStatisticsService clickStatisticsService;
	private final UrlAnalyticsService urlAnalyticsService;
	private final TagService tagService;

	public UrlAnalyticsFacade(MyUrlService myUrlService, ClickStatisticsService clickStatisticsService,
							  UrlAnalyticsService urlAnalyticsService, TagService tagService) {
		this.myUrlService = myUrlService;
		this.clickStatisticsService = clickStatisticsService;
		this.urlAnalyticsService = urlAnalyticsService;
		this.tagService = tagService;
	}

	/**
	 * 나의 단축 URL 목록 조회
	 */
	public MyUrlsListResult getMyUrls(MyUrlsCommand command) {

		log.info("단축 URL 목록 조회 요청: memberId={}, page={}, size={}",
				 command.getMemberId(), command.getPage(), command.getSize());

		// url 리스트 조회 (태그 필터링 분기는 MyUrlService 내부에서 처리)
		MyUrlsListInfo urlsListInfo = myUrlService.getMyUrls(command.getMemberId(), command.getPage(),
															 command.getSize(), command.getTagIds(), command.isAndMode());

		// 태그 조회
		Map<Long, List<TagInfo>> tagsMap = tagService.getTagsForUrls(urlsListInfo.getUrlIds());

		// url 리스트 id로 최근 일별 통계 조회
		RecentDailyStatisticsInfo recentDailyStatisticsInfo = clickStatisticsService.getRecentDailyStatistics(
			urlsListInfo.getUrlIds());

		return MyUrlsListResult.from(urlsListInfo, tagsMap, recentDailyStatisticsInfo);
	}

	/**
	 * URL 통계 조회
	 */
	public UrlAnalyticsResult getUrlAnalytics(UrlAnalyticsCommand command) {

		log.info("URL 통계 조회 요청: urlMappingId={}, memberId={}", command.getUrlMappingId(), command.getMemberId());

		UrlAnalyticsInfo analyticsInfo = urlAnalyticsService.getAnalytics(command.getUrlMappingId(), command.getMemberId());

		return UrlAnalyticsResult.from(analyticsInfo);
	}
}
