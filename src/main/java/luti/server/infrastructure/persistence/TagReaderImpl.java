package luti.server.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import luti.server.domain.model.Tag;
import luti.server.domain.port.TagReader;

@Component
public class TagReaderImpl implements TagReader {

	private final TagRepository tagRepository;

	public TagReaderImpl(TagRepository tagRepository) {
		this.tagRepository = tagRepository;
	}

	@Override
	public Optional<Tag> findById(Long id) {
		return tagRepository.findById(id);
	}

	@Override
	public List<Tag> findAllByMemberId(Long memberId) {
		return tagRepository.findAllByMember_Id(memberId);
	}

	@Override
	public Optional<Tag> findByMemberIdAndName(Long memberId, String name) {
		return tagRepository.findByMember_IdAndName(memberId, name);
	}

	@Override
	public long countByMemberId(Long memberId) {
		return tagRepository.countByMember_Id(memberId);
	}

}
