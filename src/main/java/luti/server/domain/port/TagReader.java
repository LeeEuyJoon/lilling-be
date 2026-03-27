package luti.server.domain.port;

import java.util.List;
import java.util.Optional;

import luti.server.domain.model.Tag;

public interface TagReader {
	Optional<Tag> findById(Long id);
	List<Tag> findAllByMemberId(Long memberId);
	Optional<Tag> findByMemberIdAndName(Long memberId, String name);
	long countByMemberId(Long memberId);
}
