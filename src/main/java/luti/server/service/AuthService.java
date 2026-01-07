package luti.server.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import luti.server.entity.Member;
import luti.server.repository.MemberRepository;

@Service
public class AuthService {

	private final MemberRepository memberRepository;

	public AuthService(MemberRepository memberRepository) {
		this.memberRepository = memberRepository;
	}

	public boolean isAuthenticated(Authentication authentication) {
		return authentication != null
			&& authentication.isAuthenticated()
			&& !"anonymousUser".equals(authentication.getPrincipal());
	}

	public Member getMemberFromAuthentication(Authentication authentication) {
		if (!isAuthenticated(authentication)) {
			return null;
		}

		Member member = memberRepository.findById(
			Long.parseLong(authentication.getName())
		).orElse(null);

		return member;
	}
}
