package luti.server.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import luti.server.Entity.UrlMapping;
import luti.server.Repository.UrlMappingRepository;

@Service
public class UrlService {

	private final UrlMappingRepository urlMappingRepository;

	@Value("${DOMAIN}")
	private String DOMAIN;

	@Value("${APP_ID}")
	private String APP_ID;

	public UrlService(UrlMappingRepository urlMappingRepository) {
		this.urlMappingRepository = urlMappingRepository;
	}

	@Transactional
	public String generateShortenedUrl(String originalUrl, Long nextId, Long scrambledId, String encodedValue) {

		UrlMapping urlMapping = UrlMapping.builder()
			.scrambledId(scrambledId)
			.kgsId(nextId)
			.originalUrl(originalUrl)
			.shortUrl(DOMAIN + "/" + encodedValue)
			.appId(APP_ID)
			.build();

		urlMappingRepository.save(urlMapping);

		return urlMapping.getShortUrl();
	}

	@Cacheable(value = "urlMapping", key = "#p0")
	public String getOriginalUrl(Long scrambledId) {
		UrlMapping urlMapping = urlMappingRepository.findByScrambledId(scrambledId).orElseThrow(() ->
			new IllegalArgumentException("No URL mapping found for scrambled ID: " + scrambledId)
		);

		return urlMapping.getOriginalUrl();
	}

}
