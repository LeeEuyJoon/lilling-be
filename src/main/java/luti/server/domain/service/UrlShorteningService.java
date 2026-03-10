package luti.server.domain.service;

import static luti.server.exception.ErrorCode.*;

import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import luti.server.domain.model.Member;
import luti.server.domain.model.UrlMapping;
import luti.server.domain.port.AtomicUrlMappingInserter;
import luti.server.domain.port.MemberReader;
import luti.server.domain.util.Base62Encoder;
import luti.server.domain.util.IdScrambler;
import luti.server.domain.util.KeywordSuffixScrambler;
import luti.server.exception.BusinessException;

@Service
public class UrlShorteningService {

	private static final Logger log = LoggerFactory.getLogger(UrlShorteningService.class);

	private final MemberReader memberReader;
	private final Base62Encoder base62Encoder;
	private final IdScrambler idScrambler;
	private final AtomicUrlMappingInserter atomicInserter;
	private final KeywordSuffixScrambler keywordSuffixScrambler;
	@Qualifier("counterRedisTemplate")
	private final RedisTemplate<String, Long> counterRedisTemplate;

	@Value("${DOMAIN}")
	private String DOMAIN;

	@Value("${APP_ID}")
	private String APP_ID;

	private static final String KEYWORD_COUNTER_KEY_PREFIX = "kw:cnt:";
	private static final int MAX_COUNTER_RETRY = 5;

	private static final Pattern BASE62_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,7}$");

	public UrlShorteningService(MemberReader memberReader, Base62Encoder base62Encoder,
								IdScrambler idScrambler, AtomicUrlMappingInserter atomicInserter,
								KeywordSuffixScrambler keywordSuffixScrambler,
								RedisTemplate<String, Long> counterRedisTemplate) {
		this.memberReader = memberReader;
		this.base62Encoder = base62Encoder;
		this.idScrambler = idScrambler;
		this.atomicInserter = atomicInserter;
		this.keywordSuffixScrambler = keywordSuffixScrambler;
		this.counterRedisTemplate = counterRedisTemplate;
	}

	public Optional<String> generateShortenedUrl(String originalUrl, Long nextId, Long scrambledId,
												  String encodedValue, Long memberId) {
		log.debug("URL 매핑 생성 시작: kgsId={}, scrambledId={}, shortCode={}", nextId, scrambledId, encodedValue);

		Member member = resolveMember(memberId);

		UrlMapping urlMapping = UrlMapping.builder()
										  .scrambledId(scrambledId)
										  .kgsId(nextId)
										  .originalUrl(originalUrl)
										  .shortUrl(DOMAIN + "/" + encodedValue)
										  .appId(APP_ID)
										  .member(member)
										  .build();

		if (atomicInserter.tryInsert(urlMapping)) {
			log.debug("URL 매핑 저장 성공: scrambledId={}, appId={}", scrambledId, APP_ID);
			return Optional.of(urlMapping.getShortUrl());
		}
		return Optional.empty();
	}

	@Transactional
	public String generateShortenedUrlWithKeyword(String originalUrl, String keyword, Long memberId) {
		log.debug("URL 매핑 생성 (키워드) 시작: keyword={}", keyword);

		validateKeyword(keyword);

		Member member = resolveMember(memberId);

		// 7글자 키워드: suffix 공간 없음 → 최초 요청자만 해당 shortCode 획득 (선점)
		if (keyword.length() == 7) {
			UrlMapping urlMapping = buildKeywordUrlMapping(originalUrl, keyword, member);
			if (atomicInserter.tryInsert(urlMapping)) {
				return urlMapping.getShortUrl();
			}
			throw new BusinessException(CANNOT_USE_KEYWORD);
		}

		// redis counter로 고유한 N 발급 -> Feistel 스크램블링 -> suffix 생성
		long suffixSpace = keywordSuffixScrambler.suffixSpace(keyword);

		for (int attempt = 0; attempt < MAX_COUNTER_RETRY; attempt++) {
			Long rawCounter = counterRedisTemplate.opsForValue()
				.increment(KEYWORD_COUNTER_KEY_PREFIX + keyword);
			long N = rawCounter;

			if (N >= suffixSpace) {
				throw new BusinessException(CANNOT_USE_KEYWORD);
			}

			long scrambled = keywordSuffixScrambler.scramble(N, keyword);
			String shortCode = keyword + base62Encoder.encode(scrambled);

			UrlMapping urlMapping = buildKeywordUrlMapping(originalUrl, shortCode, member);
			if (atomicInserter.tryInsert(urlMapping)) {
				return urlMapping.getShortUrl();
			}
			log.warn("keyword suffix insert 실패 (N={}, attempt={}), 다음 N 시도", N, attempt);
		}

		throw new BusinessException(CANNOT_USE_KEYWORD);
	}

	public void validateOriginalUrl(String originalUrl) {
		if (originalUrl == null || originalUrl.isBlank()) {
			throw new BusinessException(INVALID_ORIGINAL_URL);
		}
		if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
			throw new BusinessException(INVALID_ORIGINAL_URL);
		}
	}

	public Member resolveMember(Long memberId) {
		return Optional.ofNullable(memberId)
					   .flatMap(memberReader::findById)
					   .orElse(null);
	}

	private void validateKeyword(String keyword) {
		if (!BASE62_PATTERN.matcher(keyword).matches()) {
			throw new BusinessException(INVALID_KEYWORD_FORMAT);
		}
	}

	private UrlMapping buildKeywordUrlMapping(String originalUrl, String shortCode, Member member) {
		Long scrambledId = base62Encoder.decode(shortCode);
		Long kgsId = idScrambler.descramble(scrambledId);

		return UrlMapping.builder()
						 .scrambledId(scrambledId)
						 .kgsId(kgsId)
						 .originalUrl(originalUrl)
						 .shortUrl(DOMAIN + "/" + shortCode)
						 .appId(APP_ID)
						 .member(member)
						 .build();
	}

}
