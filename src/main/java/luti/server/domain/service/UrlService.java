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
import luti.server.domain.port.MemberReader;
import luti.server.domain.port.UrlMappingReader;
import luti.server.domain.port.UrlMappingStore;
import luti.server.domain.util.Base62Encoder;
import luti.server.domain.util.IdScrambler;
import luti.server.exception.BusinessException;
import luti.server.domain.service.dto.UrlMappingInfo;

@Service
public class UrlService {

	private static final Logger log = LoggerFactory.getLogger(UrlService.class);

	private final UrlMappingReader urlMappingReader;
	private final UrlMappingStore urlMappingStore;
	private final MemberReader memberReader;
	private final Base62Encoder base62Encoder;
	private final IdScrambler idScrambler;
	private final KeywordUrlInserter keywordUrlInserter;

	@Value("${DOMAIN}")
	private String DOMAIN;

	@Value("${APP_ID}")
	private String APP_ID;

	private static final Pattern BASE62_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,7}$");

	public UrlService(UrlMappingReader urlMappingReader,
					  UrlMappingStore urlMappingStore, MemberReader memberReader, Base62Encoder base62Encoder,
					  IdScrambler idScrambler, KeywordUrlInserter keywordUrlInserter) {
		this.urlMappingReader = urlMappingReader;
		this.urlMappingStore = urlMappingStore;
		this.memberReader = memberReader;
		this.base62Encoder = base62Encoder;
		this.idScrambler = idScrambler;
		this.keywordUrlInserter = keywordUrlInserter;
	}

	@Transactional
	public String generateShortenedUrl(String originalUrl, Long nextId, Long scrambledId, String encodedValue,
									   Long memberId) {
		log.debug("URL 매핑 생성 시작: kgsId={}, scrambledId={}, shortCode={}", nextId, scrambledId, encodedValue);

		Member member = Optional.ofNullable(memberId)
								.flatMap(memberReader::findById)
								.orElse(null);

		UrlMapping urlMapping = UrlMapping.builder()
										  .scrambledId(scrambledId)
										  .kgsId(nextId)
										  .originalUrl(originalUrl)
										  .shortUrl(DOMAIN + "/" + encodedValue)
										  .appId(APP_ID)
										  .member(member)
										  .build();

		urlMappingStore.save(urlMapping);
		log.debug("URL 매핑 저장 성공: scrambledId={}, appId={}", scrambledId, APP_ID);

		return urlMapping.getShortUrl();
	}

	@Transactional
	public String generateShortenedUrlWithKeyword(String originalUrl, String keyword, Long memberId) {

		log.debug("URL 매핑 생성 (키워드) 시작: keyword={}", keyword);

		// 키워드를 사용할 수 있는지 확인하려면 키워드 자체를 shortCode로 쓰고 DB에 쓰기작업을 시도 (lill.ing/keyword)
		// 만약 unique 제약조건에 걸리면 base62 기준으로 1씩 증가시키면서 재시도 (lill.ing/keyword1, lill.ing/keyword2, ...)

		validateKeyword(keyword);

		int suffixMaxLen = 7 - keyword.length();
		if (suffixMaxLen == 0) {
			// 키워드가 7자리면 그 키워드를 shortCode로 바로 사용

			Member member = Optional.ofNullable(memberId)
									.flatMap(memberReader::findById)
									.orElse(null);

			Long scrambledId = base62Encoder.decode(keyword);
			Long kgsId = idScrambler.descramble(scrambledId);

			UrlMapping urlMapping = UrlMapping.builder()
											  .scrambledId(scrambledId)
											  .kgsId(kgsId)
											  .originalUrl(originalUrl)
											  .shortUrl(DOMAIN + "/" + keyword)
											  .appId(APP_ID)
											  .member(member)
											  .build();

			try {
				urlMappingStore.saveAndFlush(urlMapping);
				return urlMapping.getShortUrl();
			} catch (Exception e) {
				throw new BusinessException(CANNOT_USE_KEYWORD);
			}
		}

		// 키워드가 7자리보다 짧으면 suffix를 붙여가면서 시도

		String shortUrl = keywordUrlInserter.tryInsertKeywordMapping(originalUrl, keyword, memberId);
		if (shortUrl != null) {
			return shortUrl;
		}

		for (long i = 0; ; i++) {
			String suffix = base62Encoder.encode(i);
			if (suffix.length() > suffixMaxLen) break;

			String candidateShortCode = keyword + suffix;
			shortUrl = keywordUrlInserter.tryInsertKeywordMapping(originalUrl, candidateShortCode, memberId);
			if (shortUrl != null) {
				return shortUrl;
			}
		}
		throw new BusinessException(CANNOT_USE_KEYWORD);
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

}
