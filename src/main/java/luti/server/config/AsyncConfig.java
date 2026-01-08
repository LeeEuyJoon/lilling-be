package luti.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

	private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (throwable, method, params) -> {
			log.error("비동기 메서드 예외 발생: method={}, class={}, params={}",
					  method.getName(),
					  method.getDeclaringClass().getSimpleName(),
					  params,
					  throwable);
		};
	}

}