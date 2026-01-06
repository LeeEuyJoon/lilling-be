package luti.server.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import luti.server.entity.Member;
import luti.server.enums.Provider;
import luti.server.repository.MemberRepository;
import luti.server.service.dto.ProvisionedMemberDto;

@Service
public class MemberProvisionService {

	private final MemberRepository memberRepository;

	public MemberProvisionService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	@Transactional
	public ProvisionedMemberDto findOrCreate(Provider provider, String providerSubject, String email) {

		Member existing = memberRepository
			.findByProviderAndProviderSubject(provider, providerSubject)
			.orElse(null);

		if (existing != null) {
			existing.updateEmailIfPresent(email);
			return new ProvisionedMemberDto(existing.getId(), existing.getRole());
		}

		// 신규 가입
		Member member = new Member(provider, providerSubject, email);
		memberRepository.save(member);

		return new ProvisionedMemberDto(member.getId(), member.getRole());
	}
}
