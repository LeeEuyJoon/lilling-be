package luti.server.domain.port;

import java.util.Optional;

import luti.server.domain.model.Member;

public interface MemberReader {

	Optional<Member> findById(Long id);

}
