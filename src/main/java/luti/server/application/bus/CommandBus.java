package luti.server.application.bus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import luti.server.application.command.ICommand;
import luti.server.application.handler.CommandHandler;

@Component
public class CommandBus {

	private final Map<Class<?>, CommandHandler<?, ?>> handlerMap = new HashMap<>();

	public CommandBus(List<CommandHandler<?, ?>> handlers) {
		for (CommandHandler<?, ?> handler: handlers) {
			handlerMap.put(handler.getSupportedCommandType(), handler);
		}
	}

	public <R, T extends ICommand<R>> R execute(T command) {
		CommandHandler<T, R> handler = (CommandHandler<T, R>) handlerMap.get(command.getClass());
		if (handler == null) {
			throw new IllegalArgumentException("처리할 핸들러가 없습니다: " + command.getClass().getSimpleName());
		}
		return handler.execute(command);
	}

}
