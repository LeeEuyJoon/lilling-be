package luti.server.application.handler.query;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.handler.QueryHandler;
import luti.server.application.query.MyUrlsQuery;
import luti.server.application.result.MyUrlsListResult;
import luti.server.domain.service.ClickStatisticsService;
import luti.server.domain.service.MyUrlService;
import luti.server.domain.service.TagService;
import luti.server.domain.service.dto.MyUrlsListInfo;
import luti.server.domain.service.dto.RecentDailyStatisticsInfo;
import luti.server.domain.service.dto.TagInfo;

@Component
public class MyUrlsQueryHandler implements QueryHandler<MyUrlsQuery, MyUrlsListResult> {

	private static final Logger log = LoggerFactory.getLogger(MyUrlsQueryHandler.class);

	private final MyUrlService myUrlService;
	private final ClickStatisticsService clickStatisticsService;
	private final TagService tagService;

	public MyUrlsQueryHandler(MyUrlService myUrlService, ClickStatisticsService clickStatisticsService,
							  TagService tagService) {
		this.myUrlService = myUrlService;
		this.clickStatisticsService = clickStatisticsService;
		this.tagService = tagService;
	}

	@Override
	public MyUrlsListResult execute(MyUrlsQuery query) {

		log.info("단축 URL 목록 조회 요청: memberId={}, page={}, size={}",
				 query.getMemberId(), query.getPage(), query.getSize());

		MyUrlsListInfo urlsListInfo = myUrlService.getMyUrls(query.getMemberId(), query.getPage(),
															 query.getSize(), query.getTagIds(), query.isAndMode());

		Map<Long, List<TagInfo>> tagsMap = tagService.getTagsForUrls(urlsListInfo.getUrlIds());

		RecentDailyStatisticsInfo recentDailyStatisticsInfo = clickStatisticsService.getRecentDailyStatistics(
			urlsListInfo.getUrlIds());

		return MyUrlsListResult.from(urlsListInfo, tagsMap, recentDailyStatisticsInfo);
	}

	@Override
	public Class<MyUrlsQuery> getSupportedQueryType() {
		return MyUrlsQuery.class;
	}
}
