package luti.server.domain.port;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import luti.server.domain.model.UrlMapping;

public interface UrlMappingReader {

	Optional<UrlMapping> findById(Long id);

	Optional<UrlMapping> findByScrambledId(Long scrambledId);

	Page<UrlMapping> findByMemberId(Long memberId, Pageable pageable);

	Page<UrlMapping> findByMemberIdAndTagIds(Long memberId, List<Long> tagIds, Pageable pageable);
}
