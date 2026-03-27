package luti.server.application.facade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.command.AssignTagsCommand;
import luti.server.application.command.CreateTagCommand;
import luti.server.application.command.DeleteTagCommand;
import luti.server.application.result.TagListResult;
import luti.server.application.result.TagResult;
import luti.server.application.command.UnassignTagsCommand;
import luti.server.application.command.UpdateTagCommand;
import luti.server.domain.service.TagService;

@Component
public class TagFacade {

	private static final Logger log = LoggerFactory.getLogger(TagFacade.class);

	private final TagService tagService;

	public TagFacade(TagService tagService) {
		this.tagService = tagService;
	}

	public TagListResult getTags(Long memberId) {
		return TagListResult.from(tagService.getTagsByMember(memberId));
	}
	public TagResult createTag(CreateTagCommand command) {
		return TagResult.from(tagService.createTag(command.getMemberId(), command.getName()));
	}
	public void updateTag(UpdateTagCommand command) {
		tagService.updateTag(command.getMemberId(), command.getTagId(), command.getName());
	}
	public void deleteTag(DeleteTagCommand command) {
		tagService.deleteTag(command.getMemberId(), command.getTagId());
	}
	public void assignTags(AssignTagsCommand command) {
		tagService.assignTags(command.getMemberId(), command.getUrlId(), command.getTagIds());
	}
	public void unassignTags(UnassignTagsCommand command) {
		tagService.unassignTags(command.getMemberId(), command.getUrlId(), command.getTagIds());
	}
}
