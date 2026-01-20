package luti.server.domain.port;

import luti.server.domain.model.Member;

public interface MemberStore {

	Member save(Member member);

}
