package luti.server.domain.port;

import luti.server.domain.model.Tag;

public interface TagStore {
	Tag save(Tag tag);
	void deleteById(Long id);
}
