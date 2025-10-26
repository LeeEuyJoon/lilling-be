package luti.server.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import luti.server.Entity.UrlMapping;
import luti.server.Repository.UrlMappingRepository;

@Service
public class UrlService {

	private final UrlMappingRepository urlMappingRepository;

	@Value("${DOMAIN}")
	private String DOMAIN;

	public UrlService(UrlMappingRepository urlMappingRepository) {
		this.urlMappingRepository = urlMappingRepository;
	}

	@Transactional
	public String generateShortenedUrl(String originalUrl, Long scrambledId, String encodedValue) {

		UrlMapping urlMapping = UrlMapping.builder()
			.scrambledId(scrambledId)
			.originalUrl(originalUrl)
			.shortUrl(DOMAIN + "/" + encodedValue)
			.build();

		urlMappingRepository.save(urlMapping);

		return urlMapping.getShortUrl();
	}

	public String getOriginalUrl(String shortCode) {
		UrlMapping urlMapping = urlMappingRepository.findByShortUrl(DOMAIN + "/" + shortCode);

		return urlMapping.getOriginalUrl();
	}
}
