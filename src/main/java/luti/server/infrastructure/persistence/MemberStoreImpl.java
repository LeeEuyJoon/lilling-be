package luti.server.infrastructure.persistence;

import org.springframework.stereotype.Component;

import luti.server.domain.model.Member;
import luti.server.domain.port.MemberStore;

@Component
public class MemberStoreImpl implements MemberStore {

	private final MemberRepository repository;

	public MemberStoreImpl(MemberRepository repository) {
		this.repository = repository;
	}

	@Override
	public Member save(Member member) {
		return repository.save(member);
	}
}
