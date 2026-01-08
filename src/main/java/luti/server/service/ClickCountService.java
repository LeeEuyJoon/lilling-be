package luti.server.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import luti.server.repository.UrlMappingRepository;

@Service
public class ClickCountService {

	private final UrlMappingRepository urlMappingRepository;

	public ClickCountService(UrlMappingRepository urlMappingRepository) {
		this.urlMappingRepository = urlMappingRepository;
	}

	@Async
	@Transactional
	public void increaseClickCount(Long urlMappingId) {
		urlMappingRepository.incrementClickCount(urlMappingId);
	}
}
