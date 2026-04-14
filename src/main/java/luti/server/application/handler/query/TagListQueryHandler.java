package luti.server.application.handler.query;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.handler.QueryHandler;
import luti.server.application.query.TagListQuery;
import luti.server.application.result.TagListResult;
import luti.server.domain.service.TagService;
import luti.server.domain.service.dto.TagInfo;

@Component
public class TagListQueryHandler implements QueryHandler<TagListQuery, TagListResult> {

	private static final Logger log = LoggerFactory.getLogger(TagListQueryHandler.class);

	private final TagService tagService;

	public TagListQueryHandler(TagService tagService) {
		this.tagService = tagService;
	}

	@Override
	public TagListResult execute(TagListQuery query) {

		log.info("태그 목록 조회 요청: memberId={}", query.getMemberId());

		List<TagInfo> tags = tagService.getTagsByMember(query.getMemberId());
		TagListResult result = TagListResult.from(tags);
		return result;
	}

	@Override
	public Class<TagListQuery> getSupportedQueryType() {
		return TagListQuery.class;
	}
}
