package luti.server.application.handler.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import luti.server.application.command.legacy.DeleteUrlCommand;
import luti.server.application.handler.CommandHandler;
import luti.server.domain.service.MyUrlService;

@Component
public class DeleteUrlCommandHandler implements CommandHandler<DeleteUrlCommand, Void> {

	private static final Logger log = LoggerFactory.getLogger(DeleteUrlCommandHandler.class);

	private final MyUrlService myUrlService;

	public DeleteUrlCommandHandler(MyUrlService myUrlService) {
		this.myUrlService = myUrlService;
	}

	@Override
	public Void execute(DeleteUrlCommand command) {

		log.info("단축 URL 삭제 요청: memberId={}, urlId={}", command.getMemberId(), command.getUrlId());

		myUrlService.deleteUrlMapping(command.getUrlId(), command.getMemberId());

		return null;
	}

	@Override
	public Class<DeleteUrlCommand> getSupportedCommandType() {
		return DeleteUrlCommand.class;
	}
}
