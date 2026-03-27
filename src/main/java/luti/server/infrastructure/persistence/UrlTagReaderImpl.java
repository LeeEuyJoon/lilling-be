package luti.server.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Component;

import luti.server.domain.model.UrlTag;
import luti.server.domain.port.UrlTagReader;

@Component
public class UrlTagReaderImpl implements UrlTagReader {

	private final UrlTagRepository urlTagRepository;

	public UrlTagReaderImpl(UrlTagRepository urlTagRepository) {
		this.urlTagRepository = urlTagRepository;
	}

	@Override
	public List<UrlTag> findByUrlId(Long urlMappingId) {
		return urlTagRepository.findByUrlMapping_Id(urlMappingId);
	}

	@Override
	public List<UrlTag> findByUrlMappingId(List<Long> urlMappingIds) {
		return urlTagRepository.findByUrlMapping_IdIn(urlMappingIds);
	}
}
