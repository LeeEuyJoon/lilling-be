package luti.server.application.handler;

import luti.server.application.query.IQuery;

public interface QueryHandler<T extends IQuery<R>, R> {
	R execute(T query);
	Class<T> getSupportedQueryType();
}
