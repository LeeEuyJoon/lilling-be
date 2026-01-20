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
import luti.server.exception.BusinessException;
import luti.server.domain.service.dto.UrlMappingInfo;

@Service
public class UrlService {

	private static final Logger log = LoggerFactory.getLogger(UrlService.class);

	private final UrlMappingReader urlMappingReader;
	private final UrlMappingStore urlMappingStore;
	private final MemberReader memberReader;

	@Value("${DOMAIN}")
	private String DOMAIN;

	@Value("${APP_ID}")
	private String APP_ID;

	private static final Pattern BASE62_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,7}$");

	public UrlService(UrlMappingReader urlMappingReader,
					  UrlMappingStore urlMappingStore, MemberReader memberReader) {
		this.urlMappingReader = urlMappingReader;
		this.urlMappingStore = urlMappingStore;
		this.memberReader = memberReader;
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
		log.info("URL 매핑 저장 성공: scrambledId={}, appId={}", scrambledId, APP_ID);

		return urlMapping.getShortUrl();
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
