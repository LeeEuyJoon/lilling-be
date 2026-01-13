package luti.server.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import luti.server.domain.model.Member;
import luti.server.domain.enums.Provider;

public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findByProviderAndProviderSubject(Provider provider, String providerSubject);
}
