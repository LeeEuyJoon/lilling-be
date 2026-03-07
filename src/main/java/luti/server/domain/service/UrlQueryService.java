package luti.server.domain.service;

import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import luti.server.domain.model.UrlMapping;
import luti.server.domain.port.UrlMappingReader;
import luti.server.domain.service.dto.UrlMappingInfo;
import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;

@Service
public class UrlQueryService {

	private static final Logger log = LoggerFactory.getLogger(UrlQueryService.class);

	private final UrlMappingReader urlMappingReader;

	@Value("${DOMAIN}")
	private String DOMAIN;

	private static final Pattern BASE62_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,7}$");

	public UrlQueryService(UrlMappingReader urlMappingReader) {
		this.urlMappingReader = urlMappingReader;
	}

	@Cacheable(value = "urlMapping", key = "#p0")
	public String getOriginalUrl(Long scrambledId) {
		log.debug("URL 조회 시작: scrambledId={}", scrambledId);

		UrlMapping urlMapping = urlMappingReader.findByScrambledId(scrambledId).orElseThrow(
			() -> {
				log.warn("URL을 찾을 수 없음: scrambledId={}", scrambledId);
				return new BusinessException(ErrorCode.URL_NOT_FOUND);
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
