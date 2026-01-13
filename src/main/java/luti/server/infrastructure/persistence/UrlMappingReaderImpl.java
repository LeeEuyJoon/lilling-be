package luti.server.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import luti.server.domain.model.UrlMapping;
import luti.server.domain.repository.UrlMappingReader;

@Component
public class UrlMappingReaderImpl implements UrlMappingReader {

	private final UrlMappingRepository repository;

	public UrlMappingReaderImpl(UrlMappingRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<UrlMapping> findById(Long id) {
		return repository.findById(id);
	}

	@Override
	public Optional<UrlMapping> findByScrambledId(Long scrambledId) {
		return repository.findByScrambledId(scrambledId);
	}

	@Override
	public Page<UrlMapping> findByMemberId(Long memberId, Pageable pageable) {
		return repository.findByMember_IdOrderByCreatedAtDesc(memberId, pageable);
	}
}
