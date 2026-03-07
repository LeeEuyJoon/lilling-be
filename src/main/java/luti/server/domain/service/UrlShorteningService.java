package luti.server.domain.service;

import static luti.server.exception.ErrorCode.*;

import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

	@Value("${DOMAIN}")
	private String DOMAIN;

	@Value("${APP_ID}")
	private String APP_ID;

	private static final Pattern BASE62_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,7}$");

	public UrlShorteningService(MemberReader memberReader, Base62Encoder base62Encoder,
								IdScrambler idScrambler, AtomicUrlMappingInserter atomicInserter,
								KeywordSuffixScrambler keywordSuffixScrambler) {
		this.memberReader = memberReader;
		this.base62Encoder = base62Encoder;
		this.idScrambler = idScrambler;
		this.atomicInserter = atomicInserter;
		this.keywordSuffixScrambler = keywordSuffixScrambler;
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

		// keyword 자체 먼저 시도
		UrlMapping urlMapping = buildKeywordUrlMapping(originalUrl, keyword, member);
		if (atomicInserter.tryInsert(urlMapping)) {
			return urlMapping.getShortUrl();
		}

		// suffix 공간이 없는 경우 바로 실패 (keyword가 7글자인 경우)
		if (keyword.length() == 7) {
			throw new BusinessException(CANNOT_USE_KEYWORD);
		}

		// 파이스텔 스크램블링된 순서로 suffix 탐색
		long suffixSpace = keywordSuffixScrambler.suffixSpace(keyword);
		for (long i = 0; i < suffixSpace; i++) {
			long scrambledI = keywordSuffixScrambler.scramble(i, keyword);
			String suffix = base62Encoder.encode(scrambledI);

			urlMapping = buildKeywordUrlMapping(originalUrl, keyword + suffix, member);
			if (atomicInserter.tryInsert(urlMapping)) {
				return urlMapping.getShortUrl();
			}
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
