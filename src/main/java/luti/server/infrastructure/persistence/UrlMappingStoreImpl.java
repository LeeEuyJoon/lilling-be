package luti.server.infrastructure.persistence;

import org.springframework.stereotype.Component;

import luti.server.domain.model.UrlMapping;
import luti.server.domain.repository.UrlMappingStore;

@Component
public class UrlMappingStoreImpl implements UrlMappingStore {

	private final UrlMappingRepository repository;

	public UrlMappingStoreImpl(UrlMappingRepository repository) {
		this.repository = repository;
	}

	@Override
	public UrlMapping save(UrlMapping urlMapping) {
		return repository.save(urlMapping);
	}

	@Override
	public void deleteById(Long id) {
		repository.deleteById(id);
	}

	@Override
	public void updateDescription(Long urlMappingId, String description) {
		repository.updateDescriptionById(urlMappingId, description);
	}

	@Override
	public void claimToMember(Long urlMappingId, Long memberId) {
		repository.claimUrlMappingToMemberById(urlMappingId, memberId);
	}

	@Override
	public void incrementClickCount(Long scrambledId) {
		repository.incrementClickCount(scrambledId);
	}
}
