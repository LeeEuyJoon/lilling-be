package luti.server.application.handler.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.command.UnassignTagsCommand;
import luti.server.application.handler.CommandHandler;
import luti.server.domain.service.MyUrlService;
import luti.server.domain.service.TagService;

@Component
public class UnAssignsTagHandler implements CommandHandler<UnassignTagsCommand, Void> {

	private static final Logger log = LoggerFactory.getLogger(UnAssignsTagHandler.class);

	private final MyUrlService myUrlService;
	private final TagService tagService;

	public UnAssignsTagHandler(MyUrlService myUrlService, TagService tagService) {
		this.myUrlService = myUrlService;
		this.tagService = tagService;
	}

	@Override
	public Void execute(UnassignTagsCommand command) {

		log.info("태그 할당 해제 요청: memberId={}, urlId={}, tagIds={}", command.getMemberId(), command.getUrlId(),
				 command.getTagIds());

		// UrlMapping 소유자 검증
		myUrlService.isUrlOwnedByMember(command.getUrlId(), command.getMemberId());

		// 태그 할당 해제
		tagService.unassignTags(command.getMemberId(), command.getUrlId(), command.getTagIds());

		return null;
	}

	@Override
	public Class<UnassignTagsCommand> getSupportedCommandType() {
		return UnassignTagsCommand.class;
	}
}
