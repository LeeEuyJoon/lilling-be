package luti.server.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

	@Bean
	public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
		RedisCacheConfiguration config =
			RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofDays(7))  // 캐시의 기본 만료 시간 설정
				.serializeKeysWith(
					RedisSerializationContext.SerializationPair.fromSerializer(
						new StringRedisSerializer()
					)
				)
				.serializeValuesWith(
					RedisSerializationContext.SerializationPair.fromSerializer(
						new
							GenericJackson2JsonRedisSerializer()
					)
				)
				.disableCachingNullValues();  //null 값은 캐시하지 않음

		return
			RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(config)
				.build();
	}
}
