package luti.server.domain.port;

import java.util.List;

import luti.server.domain.model.UrlTag;

public interface UrlTagReader {
	List<UrlTag> findByUrlMappingId(Long urlMappingId);
	List<UrlTag> findByUrlMappingIdIn(List<Long> urlMappingIds);
}
