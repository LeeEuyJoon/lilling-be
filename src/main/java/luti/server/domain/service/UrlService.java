package luti.server.domain.service;

import static luti.server.exception.ErrorCode.*;

import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import luti.server.domain.model.Member;
import luti.server.domain.model.UrlMapping;
import luti.server.domain.port.AtomicUrlMappingInserter;
import luti.server.domain.port.MemberReader;
import luti.server.domain.port.UrlMappingReader;
import luti.server.domain.util.Base62Encoder;
import luti.server.domain.util.IdScrambler;
import luti.server.exception.BusinessException;
import luti.server.domain.service.dto.UrlMappingInfo;

@Service
public class UrlService {

	private static final Logger log = LoggerFactory.getLogger(UrlService.class);

	private final UrlMappingReader urlMappingReader;
	private final MemberReader memberReader;
	private final Base62Encoder base62Encoder;
	private final IdScrambler idScrambler;
	private final AtomicUrlMappingInserter atomicInserter;

	@Value("${DOMAIN}")
	private String DOMAIN;

	@Value("${APP_ID}")
	private String APP_ID;

	private static final Pattern BASE62_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,7}$");

	public UrlService(UrlMappingReader urlMappingReader,
					  MemberReader memberReader, Base62Encoder base62Encoder,
					  IdScrambler idScrambler, AtomicUrlMappingInserter atomicInserter) {
		this.urlMappingReader = urlMappingReader;
		this.memberReader = memberReader;
		this.base62Encoder = base62Encoder;
		this.idScrambler = idScrambler;
		this.atomicInserter = atomicInserter;
	}

	public Optional<String> generateShortenedUrl(String originalUrl, Long nextId, Long scrambledId,
												  String encodedValue, Member member) {
		log.debug("URL 매핑 생성 시작: kgsId={}, scrambledId={}, shortCode={}", nextId, scrambledId, encodedValue);

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
		int suffixMaxLen = 7 - keyword.length();

		// keyword 자체 먼저 시도
		UrlMapping urlMapping = buildKeywordUrlMapping(originalUrl, keyword, member);
		if (atomicInserter.tryInsert(urlMapping)) {
			return urlMapping.getShortUrl();
		}

		// keyword + Base62 인코딩된 숫자 조합 시도 (keyword 자체가 이미 사용 중인 경우)
		for (long i = 0; ; i++) {
			String suffix = base62Encoder.encode(i);
			if (suffix.length() > suffixMaxLen) break;

			urlMapping = buildKeywordUrlMapping(originalUrl, keyword + suffix, member);
			if (atomicInserter.tryInsert(urlMapping)) {
				return urlMapping.getShortUrl();
			}
		}
		throw new BusinessException(CANNOT_USE_KEYWORD);
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

	@Cacheable(value = "urlMapping", key = "#p0")
	public String getOriginalUrl(Long scrambledId) {
		log.debug("URL 조회 시작: scrambledId={}", scrambledId);

		UrlMapping urlMapping = urlMappingReader.findByScrambledId(scrambledId).orElseThrow(
			() -> {
				log.warn("URL을 찾을 수 없음: scrambledId={}", scrambledId);
				return new BusinessException(URL_NOT_FOUND);
			}
		);

		log.debug("Redis 캐시 미스 또는 DB 조회 완료: scrambledId={}", scrambledId);
		return urlMapping.getOriginalUrl();
	}

	public Optional<UrlMappingInfo> findByDecodedId(Long scrambledId) {
		log.debug("UrlMappingInfo 조회: scrambledId={}", scrambledId);
		return urlMappingReader.findByScrambledId(scrambledId)
								   .map(UrlMappingInfo::from);
	}

	public Optional<String> verifyAndExtractShortCode(String url) {
		log.debug("URL 형식 검증 및 shortCode 추출 시작: url={}", url);

		if (url == null || url.isBlank()) {
			return Optional.empty();
		}

		// 프로토콜 제거 (http://, https:// 제거)
		String urlWithoutProtocol = url.replaceFirst("^https?://", "");

		// DOMAIN으로 시작하지 않으면 실패
		if (!urlWithoutProtocol.startsWith(DOMAIN + "/")) {
			return Optional.empty();
		}

		// shortCode 추출
		String shortCode = urlWithoutProtocol.substring(DOMAIN.length() + 1);

		// Base62 형식 검증
		if (!BASE62_PATTERN.matcher(shortCode).matches()) {
			return Optional.empty();
		}

		log.debug("URL 형식 검증 성공: url={}, shortCode={}", url, shortCode);
		return Optional.of(shortCode);
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
