package luti.server.application.handler.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.command.legacy.DescriptionCommand;
import luti.server.application.handler.CommandHandler;
import luti.server.domain.service.MyUrlService;

@Component
public class UpdateDescriptionCommandHandler implements CommandHandler<DescriptionCommand, Void> {

	private static final Logger log = LoggerFactory.getLogger(UpdateDescriptionCommandHandler.class);

	private final MyUrlService myUrlService;

	public UpdateDescriptionCommandHandler(MyUrlService myUrlService) {
		this.myUrlService = myUrlService;
	}

	@Override
	public Void execute(DescriptionCommand command) {

		log.info("단축 URL Description 수정 요청: memberId={}, urlId={}, description={}",
				 command.getMemberId(), command.getUrlId(), command.getDescription());

		myUrlService.updateUrlDescription(command.getUrlId(), command.getMemberId(), command.getDescription());

		return null;
	}

	@Override
	public Class<DescriptionCommand> getSupportedCommandType() {
		return DescriptionCommand.class;
	}
}
