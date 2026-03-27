package luti.server.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import luti.server.domain.model.UrlTag;
import luti.server.domain.port.UrlTagStore;

@Component
public class UrlTagStoreImpl implements UrlTagStore {

	private final UrlTagRepository urlTagRepository;

	public UrlTagStoreImpl(UrlTagRepository urlTagRepository) {
		this.urlTagRepository = urlTagRepository;
	}

	@Override
	public void saveAll(List<UrlTag> urlTags) {
		urlTagRepository.saveAll(urlTags);
	}

	@Override
	public void deleteByUrlMappingIdAndTagIdIn(Long urlMappingId, List<Long> tagIds) {
		urlTagRepository.deleteByUrlMappingIdAndTagIdIn(urlMappingId, tagIds);
	}

	@Override
	public void deleteByTagId(Long tagId) {
		urlTagRepository.deleteByTagId(tagId);
	}
}
