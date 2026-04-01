package luti.server.application.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

import luti.server.application.command.AssignTagsCommand;
import luti.server.application.command.CreateTagCommand;
import luti.server.application.command.DeleteTagCommand;
import luti.server.application.result.TagListResult;
import luti.server.application.result.TagResult;
import luti.server.application.command.UnassignTagsCommand;
import luti.server.application.command.UpdateTagCommand;
import luti.server.domain.service.MyUrlService;
import luti.server.domain.service.TagService;
import luti.server.domain.service.dto.TagInfo;

@Component
public class TagFacade {

	private static final Logger log = LoggerFactory.getLogger(TagFacade.class);

	private final TagService tagService;
	private final MyUrlService myUrlService;

	public TagFacade(TagService tagService, MyUrlService myUrlService) {
		this.tagService = tagService;
		this.myUrlService = myUrlService;
	}

	/**
	 * 회원의 태그 목록 조회
	 */
	public TagListResult getTags(Long memberId) {

		log.info("태그 목록 조회 요청: memberId={}", memberId);

		List<TagInfo> tags = tagService.getTagsByMember(memberId);
		TagListResult result = TagListResult.from(tags);
		return result;
	}

	/**
	 * 태그 생성
	 */
	public TagResult createTag(CreateTagCommand command) {

		log.info("태그 생성 요청: memberId={}, name={}", command.getMemberId(), command.getName());

		TagInfo tagInfo = tagService.createTag(command.getMemberId(), command.getName());
		TagResult result = TagResult.from(tagInfo);
		return result;
	}

	/**
	 * 태그 수정
	 */
	public void updateTag(UpdateTagCommand command) {

		log.info("태그 수정 요청: memberId={}, tagId={}, name={}", command.getMemberId(), command.getTagId(),
				 command.getName());

		tagService.updateTag(command.getMemberId(), command.getTagId(), command.getName());
	}

	/**
	 * 태그 삭제
	 */
	public void deleteTag(DeleteTagCommand command) {

		log.info("태그 삭제 요청: memberId={}, tagId={}", command.getMemberId(), command.getTagId());

		tagService.deleteTag(command.getMemberId(), command.getTagId());
	}

	/**
	 * 태그 할당
	 */
	public void assignTags(AssignTagsCommand command) {

		log.info("태그 할당 요청: memberId={}, urlId={}, tagIds={}", command.getMemberId(), command.getUrlId(),
				 command.getTagIds());

		// UrlMapping 소유자 검증
		myUrlService.isUrlOwnedByMember(command.getUrlId(), command.getMemberId());

		// 태그 할당
		tagService.assignTags(command.getMemberId(), command.getUrlId(), command.getTagIds());
	}

	/**
	 * 태그 할당 해제
	 */
	public void unassignTags(UnassignTagsCommand command) {

		log.info("태그 할당 해제 요청: memberId={}, urlId={}, tagIds={}", command.getMemberId(), command.getUrlId(),
				 command.getTagIds());

		// UrlMapping 소유자 검증
		myUrlService.isUrlOwnedByMember(command.getUrlId(), command.getMemberId());

		// 태그 할당 해제
		tagService.unassignTags(command.getMemberId(), command.getUrlId(), command.getTagIds());
	}
}
