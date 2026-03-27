package luti.server.infrastructure.persistence;

import org.springframework.stereotype.Component;

import luti.server.domain.model.Tag;
import luti.server.domain.port.TagStore;

@Component
public class TagStoreImpl implements TagStore {

	private final TagRepository tagRepository;

	public TagStoreImpl(TagRepository tagRepository) {
		this.tagRepository = tagRepository;
	}

	@Override
	public Tag save(Tag tag) {
		return tagRepository.save(tag);
	}

	@Override
	public void deleteById(Long id) {
		tagRepository.deleteById(id);
	}
}
