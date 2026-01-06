package luti.server.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import luti.server.entity.Member;
import luti.server.enums.Provider;

public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findByProviderAndProviderSubject(Provider provider, String providerSubject);
}
