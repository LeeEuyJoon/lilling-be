package luti.server.domain.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import luti.server.domain.repository.UrlMappingStore;

@Service
public class ClickCountService {

	private final UrlMappingStore urlMappingStore;

	public ClickCountService(UrlMappingStore urlMappingStore) {
		this.urlMappingStore = urlMappingStore;
	}

	@Async
	@Transactional
	public void increaseClickCount(Long urlMappingId) {
		urlMappingStore.incrementClickCount(urlMappingId);
	}
}
