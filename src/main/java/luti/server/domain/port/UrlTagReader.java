package luti.server.domain.port;

import java.util.List;

import luti.server.domain.model.UrlTag;

public interface UrlTagReader {
	List<UrlTag> findByUrlId(Long urlMappingId);
	List<UrlTag> findByUrlMappingId(List<Long> urlMappingIds);
}
