package luti.server.application.bus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import luti.server.application.handler.QueryHandler;
import luti.server.application.query.IQuery;

@Component
public class QueryBus {

	private final Map<Class<?>, QueryHandler<?, ?>> handlerMap = new HashMap<>();

	public QueryBus(List<QueryHandler<?, ?>> handlers) {
		for (QueryHandler<?, ?> handler: handlers) {
			handlerMap.put(handler.getSupportedQueryType(), handler);
		}
	}

	public <R, T extends IQuery<R>> R execute(T query) {
		QueryHandler<T, R> handler = (QueryHandler<T, R>) handlerMap.get(query.getClass());
		if (handler == null) {
			throw new IllegalArgumentException("처리할 핸들러가 없습니다: " + query.getClass().getSimpleName());
		}
		return handler.execute(query);
	}

}
