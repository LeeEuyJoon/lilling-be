package luti.server.application.handler.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.command.DeleteTagCommand;
import luti.server.application.handler.CommandHandler;
import luti.server.domain.service.TagService;

@Component
public class DeleteTagHandler implements CommandHandler<DeleteTagCommand, Void>  {

	private static final Logger log = LoggerFactory.getLogger(DeleteTagHandler.class);

	private final TagService tagService;

	public DeleteTagHandler(TagService tagService) {
		this.tagService = tagService;
	}

	@Override
	public Void execute(DeleteTagCommand command) {

		log.info("태그 삭제 요청: memberId={}, tagId={}", command.getMemberId(), command.getTagId());

		tagService.deleteTag(command.getMemberId(), command.getTagId());
		return null;
	}

	@Override
	public Class<DeleteTagCommand> getSupportedCommandType() {
		return DeleteTagCommand.class;
	}
}
