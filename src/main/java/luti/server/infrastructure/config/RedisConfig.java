package luti.server.infrastructure.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

	private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

	@Bean
	public RedisTemplate<String, Long> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, Long> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);

		template.setKeySerializer(new StringRedisSerializer());

		template.setValueSerializer(new GenericToStringSerializer<>(Long.class));

		template.setHashKeySerializer(new StringRedisSerializer());
		template.setHashValueSerializer(new GenericToStringSerializer<>(Long.class));

		return template;
	}

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

	@Override
	public CacheErrorHandler errorHandler() {
		return new CacheErrorHandler() {
			@Override
			public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
				log.warn("Redis 캐시 조회 실패 (DB fallback): cache={}, key={}, error={}",
					cache.getName(), key, exception.getMessage());
			}

			@Override
			public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
				log.warn("Redis 캐시 저장 실패: cache={}, key={}, error={}",
					cache.getName(), key, exception.getMessage());
			}

			@Override
			public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
				log.warn("Redis 캐시 삭제 실패: cache={}, key={}, error={}",
					cache.getName(), key, exception.getMessage());
			}

			@Override
			public void handleCacheClearError(RuntimeException exception, Cache cache) {
				log.warn("Redis 캐시 클리어 실패: cache={}, error={}",
					cache.getName(), exception.getMessage());
			}
		};
	}
}
