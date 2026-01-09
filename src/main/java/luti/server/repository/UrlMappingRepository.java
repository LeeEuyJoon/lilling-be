package luti.server.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import luti.server.entity.Member;
import luti.server.entity.UrlMapping;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

	Optional<UrlMapping> findByScrambledId(Long scrambledId);

	// 메서드 남겨놓고 나중에 원본 URL 조회와 디코딩해서 얻은 PK로 조회하는 성능 비교할 때 사용하기 (문자열 조회는 느린 이유가 있다고 한다.)
	UrlMapping findByShortUrl(String s);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.scrambledId = :scrambledId")
	void incrementClickCount(@Param("scrambledId") Long scrambledId);


	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE UrlMapping u SET u.member.id = :memberId WHERE u.id = :urlMappingId")
	void claimUrlMappingToMemberById(@Param("urlMappingId") Long urlMappingId, @Param("memberId") Long memberId);
}
