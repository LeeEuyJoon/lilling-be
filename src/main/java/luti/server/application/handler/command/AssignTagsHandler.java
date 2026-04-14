package luti.server.application.handler.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.command.AssignTagsCommand;
import luti.server.application.handler.CommandHandler;
import luti.server.domain.service.MyUrlService;
import luti.server.domain.service.TagService;

@Component
public class AssignTagsHandler implements CommandHandler<AssignTagsCommand, Void> {

	private static final Logger log = LoggerFactory.getLogger(AssignTagsHandler.class);

	private final MyUrlService myUrlService;
	private final TagService tagService;

	public AssignTagsHandler(MyUrlService myUrlService, TagService tagService) {
		this.myUrlService = myUrlService;
		this.tagService = tagService;
	}

	@Override
	public Void execute(AssignTagsCommand command) {

		log.info("태그 할당 요청: memberId={}, urlId={}, tagIds={}", command.getMemberId(), command.getUrlId(),
				 command.getTagIds());

		// UrlMapping 소유자 검증
		myUrlService.isUrlOwnedByMember(command.getUrlId(), command.getMemberId());

		// 태그 할당
		tagService.assignTags(command.getMemberId(), command.getUrlId(), command.getTagIds());

		return null;
	}

	@Override
	public Class<AssignTagsCommand> getSupportedCommandType() {
		return AssignTagsCommand.class;
	}
}
