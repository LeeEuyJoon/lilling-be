package luti.server.infrastructure.persistence;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import luti.server.domain.model.UrlMapping;
import luti.server.domain.port.AtomicUrlMappingInserter;
import luti.server.domain.port.UrlMappingStore;

@Component
public class AtomicUrlMappingInserterImpl implements AtomicUrlMappingInserter {

	private final UrlMappingStore urlMappingStore;

	public AtomicUrlMappingInserterImpl(UrlMappingStore urlMappingStore) {
		this.urlMappingStore = urlMappingStore;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public boolean tryInsert(UrlMapping urlMapping) {
		try {
			urlMappingStore.saveAndFlush(urlMapping);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
