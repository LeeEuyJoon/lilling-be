package luti.server.security;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import luti.server.entity.Member;
import luti.server.enums.Provider;
import luti.server.repository.MemberRepository;
import luti.server.security.dto.ProvisionedMemberDto;

@Component
public class OAuth2MemberProvisioner {

	private final MemberRepository memberRepository;

	public OAuth2MemberProvisioner(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	@Transactional
	public ProvisionedMemberDto findOrCreate(Provider provider, String providerSubject, String email) {

		Member existing = memberRepository
			.findByProviderAndProviderSubject(provider, providerSubject)
			.orElse(null);

		if (existing != null) {
			existing.updateEmailIfPresent(email);
			return ProvisionedMemberDto.of(existing.getId(), existing.getRole());
		}

		// 신규 가입
		Member member = new Member(provider, providerSubject, email);
		Member savedMember = memberRepository.save(member);

		return ProvisionedMemberDto.of(savedMember.getId(), member.getRole());
	}
}
