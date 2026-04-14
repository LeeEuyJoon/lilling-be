package luti.server.application.handler.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.command.CreateTagCommand;
import luti.server.application.handler.CommandHandler;
import luti.server.application.result.CreateTagResult;
import luti.server.domain.service.TagService;
import luti.server.domain.service.dto.TagInfo;

@Component
public class CreateTagHandler implements CommandHandler<CreateTagCommand, CreateTagResult> {

	private static final Logger log = LoggerFactory.getLogger(CreateTagHandler.class);

	private final TagService tagService;

	public CreateTagHandler(TagService tagService) {
		this.tagService = tagService;
	}

	@Override
	public CreateTagResult execute(CreateTagCommand command) {

		log.info("태그 생성 요청: memberId={}, name={}", command.getMemberId(), command.getName());

		TagInfo tagInfo = tagService.createTag(command.getMemberId(), command.getName());
		CreateTagResult result = CreateTagResult.from(tagInfo);

		return result;
	}

	@Override
	public Class<CreateTagCommand> getSupportedCommandType() {
		return CreateTagCommand.class;
	}
}
