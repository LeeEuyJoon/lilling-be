package luti.server.infrastructure.security;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenStore {

	private static final String PREFIX = "refresh:";

	private final StringRedisTemplate redisTemplate;

	public RefreshTokenStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	// 저장: key=refresh:{jti}, value=memberId
	public void save(String jti, String memberId, long ttlSeconds) {
		redisTemplate.opsForValue()
			.set(PREFIX + jti, memberId, Duration.ofSeconds(ttlSeconds));
	}

	// 조회: jti로 memberId 반환 (없으면 Optional.emtpy())
	public Optional<String> findMemberId(String jti) {
		String value = redisTemplate.opsForValue().get(PREFIX + jti);
		return Optional.ofNullable(value);
	}

	// 삭제: 로그아웃 시 사용
	public void delete(String jti) {
		redisTemplate.delete(PREFIX + jti);
	}
}
