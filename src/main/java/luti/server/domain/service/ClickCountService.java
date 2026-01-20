package luti.server.domain.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import luti.server.domain.port.UrlMappingStore;

@Service
public class ClickCountService {

	private final RedisTemplate<String, Long> redisTemplate;
	private final UrlMappingStore urlMappingStore;

	public ClickCountService(RedisTemplate<String, Long> redisTemplate, UrlMappingStore urlMappingStore) {
		this.redisTemplate = redisTemplate;
		this.urlMappingStore = urlMappingStore;
	}

	private static final String CLICK_COUNT_KEY_PREFIX = "click_count:";
	private static final String DIRTY_SET_KEY = "click:dirty";

	/**
	 * 클릭 수 redis 기록
	 * @param scrambledId (shortCode를 base62 디코딩한 id값, 순차 id를 스크램블링한 값, pk 아님)
	 */
	@Async
	public void recordClick(Long scrambledId) {
		String countKey = CLICK_COUNT_KEY_PREFIX + scrambledId;

		Long newCount = redisTemplate.opsForValue().increment(countKey);

		if (newCount != null && newCount == 1L) {
			redisTemplate.expire(countKey, 2, TimeUnit.HOURS);
		}

		redisTemplate.opsForSet().add(DIRTY_SET_KEY, scrambledId);
	}

	/**
	 * 기존 리다이렉트 요청마다 db에 직접 클릭 수 업데이트하던 방식
	 */
	@Async
	@Transactional
	public void increaseClickCount(Long scrambledId) {
		urlMappingStore.incrementClickCount(scrambledId);
	}

}
