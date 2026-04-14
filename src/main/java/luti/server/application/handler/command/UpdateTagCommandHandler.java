package luti.server.application.handler.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.command.UpdateTagCommand;
import luti.server.application.handler.CommandHandler;
import luti.server.domain.service.TagService;

@Component
public class UpdateTagCommandHandler implements CommandHandler<UpdateTagCommand, Void> {

	private static final Logger log = LoggerFactory.getLogger(UpdateTagCommandHandler.class);

	private final TagService tagService;

	public UpdateTagCommandHandler(TagService tagService) {
		this.tagService = tagService;
	}

	@Override
	public Void execute(UpdateTagCommand command) {

		log.info("태그 수정 요청: memberId={}, tagId={}, name={}", command.getMemberId(), command.getTagId(),
				 command.getName());

		tagService.updateTag(command.getMemberId(), command.getTagId(), command.getName());
		return null;
	}

	@Override
	public Class<UpdateTagCommand> getSupportedCommandType() {
		return UpdateTagCommand.class;
	}

}
