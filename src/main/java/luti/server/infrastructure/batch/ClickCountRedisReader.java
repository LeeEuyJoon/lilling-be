package luti.server.infrastructure.batch;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import luti.server.infrastructure.batch.dto.ClickCountData;

@Component
public class ClickCountRedisReader implements ItemReader<ClickCountData> {

	private static final Logger log = LoggerFactory.getLogger(ClickCountRedisReader.class);

	private final RedisTemplate<String, Long> redisTemplate;
	private Integer currentIndex = 0;
	private List<ClickCountData> data;

	public static final String DIRTY_SET_KEY = "click:dirty";
	private static final String CLICK_COUNT_KEY_PREFIX = "click:count:";
	private static final Integer BATCH_SIZE = 1000;

	public ClickCountRedisReader(RedisTemplate<String, Long> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public ClickCountData read() {
		if (data == null) {
			data = fetchBatchFromRedis();
			currentIndex = 0;
		}

		if (currentIndex < data.size()) {
			return data.get(currentIndex++);
		}

		return null;
	}

	private List<ClickCountData> fetchBatchFromRedis() {
		List<ClickCountData> result = new ArrayList<>();

		// dirt set에서 batch size 만큼 pop
		for (int i = 0; i < BATCH_SIZE; i++) {
			Long scrambledId = redisTemplate.opsForSet().pop(DIRTY_SET_KEY);
			if (scrambledId == null) {
				break;
			}

			// 카운트 읽고 0으로 리셋
			String countKey = CLICK_COUNT_KEY_PREFIX + scrambledId;
			Long count = redisTemplate.opsForValue().getAndSet(countKey, 0L);

			result.add(ClickCountData.of(scrambledId, count));
		}

		log.info("Redis에서 {} 개의 dirty URL 조회 완료", result.size());
		return result;

	}

}
