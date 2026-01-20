package luti.server.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import luti.server.domain.model.Member;
import luti.server.domain.port.MemberReader;

@Component
public class MemberReaderImpl implements MemberReader {

	private final MemberRepository repository;

	public MemberReaderImpl(MemberRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<Member> findById(Long id) {
		return repository.findById(id);
	}

}
