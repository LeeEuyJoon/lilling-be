package luti.server.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import org.springframework.beans.factory.annotation.Value;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Value("${CROSS_ORIGIN}")
	private String crossOrigin;

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

}
