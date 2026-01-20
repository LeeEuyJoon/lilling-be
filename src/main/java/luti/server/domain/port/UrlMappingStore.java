package luti.server.domain.port;

import luti.server.domain.model.UrlMapping;

public interface UrlMappingStore {

	UrlMapping save(UrlMapping urlMapping);

	void deleteById(Long id);

	void updateDescription(Long urlMappingId, String description);

	void claimToMember(Long urlMappingId, Long memberId);

	void incrementClickCount(Long scrambledId);
}
