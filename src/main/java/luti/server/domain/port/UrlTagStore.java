package luti.server.domain.port;

import java.util.List;

import luti.server.domain.model.UrlTag;

public interface UrlTagStore {
	void saveAll(List<UrlTag> urlTags);
	void deleteByUrlMappingIdAndTagIdIn(Long urlMappingId, List<Long> tagIds);
	void deleteByTagId(Long tagId);
}
