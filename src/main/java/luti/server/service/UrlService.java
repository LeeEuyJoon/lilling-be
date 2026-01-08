package luti.server.service;

import static luti.server.exception.ErrorCode.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import luti.server.entity.Member;
import luti.server.entity.UrlMapping;
import luti.server.repository.UrlMappingRepository;
import luti.server.exception.BusinessException;

@Service
public class UrlService {

	private static final Logger log = LoggerFactory.getLogger(UrlService.class);

	private final UrlMappingRepository urlMappingRepository;

	@Value("${DOMAIN}")
	private String DOMAIN;

	@Value("${APP_ID}")
	private String APP_ID;

	public UrlService(UrlMappingRepository urlMappingRepository) {
		this.urlMappingRepository = urlMappingRepository;
	}

	@Transactional
	public String generateShortenedUrl(String originalUrl, Long nextId, Long scrambledId, String encodedValue,
									   Member member) {
		log.debug("URL 매핑 생성 시작: kgsId={}, scrambledId={}, shortCode={}", nextId, scrambledId, encodedValue);

		UrlMapping urlMapping = UrlMapping.builder()
			.scrambledId(scrambledId)
			.kgsId(nextId)
			.originalUrl(originalUrl)
			.shortUrl(DOMAIN + "/" + encodedValue)
			.appId(APP_ID)
			.member(member)
			.build();

		urlMappingRepository.save(urlMapping);
		log.info("URL 매핑 저장 성공: scrambledId={}, appId={}", scrambledId, APP_ID);

		return urlMapping.getShortUrl();
	}

	@Cacheable(value = "urlMapping", key = "#p0")
	public String getOriginalUrl(Long scrambledId) {
		log.debug("URL 조회 시작: scrambledId={}", scrambledId);

		UrlMapping urlMapping = urlMappingRepository.findByScrambledId(scrambledId).orElseThrow(
			() -> {
				log.warn("URL을 찾을 수 없음: scrambledId={}", scrambledId);
				return new BusinessException(URL_NOT_FOUND);
			}
		);

		log.debug("Redis 캐시 미스 또는 DB 조회 완료: scrambledId={}", scrambledId);
		return urlMapping.getOriginalUrl();
	}

}
