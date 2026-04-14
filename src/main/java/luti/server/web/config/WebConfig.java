package luti.server.web.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.beans.factory.annotation.Value;

import luti.server.web.resolver.CommandArgumentResolver;
import luti.server.web.resolver.QueryArgumentResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Value("${CROSS_ORIGIN}")
	private String crossOrigin;

	private final CommandArgumentResolver commandArgumentResolver;
	private final QueryArgumentResolver queryArgumentResolver;

	public WebConfig(CommandArgumentResolver commandArgumentResolver, QueryArgumentResolver queryArgumentResolver) {
		this.commandArgumentResolver = commandArgumentResolver;
		this.queryArgumentResolver = queryArgumentResolver;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {

		String[] origins = crossOrigin.split(",");

		registry.addMapping("/api/**")
			.allowedOrigins(origins)
			.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
			.allowedHeaders("*")
			.allowCredentials(false)
			.maxAge(3600);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(commandArgumentResolver);
		resolvers.add(queryArgumentResolver);
	}

}
