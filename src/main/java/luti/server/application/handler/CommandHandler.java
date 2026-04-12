package luti.server.application.handler;

import luti.server.application.command.ICommand;

public interface CommandHandler<T extends ICommand<R>, R> {
	R execute(T command);
	Class<T> getSupportedCommandType();
}
