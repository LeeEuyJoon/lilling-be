package luti.server.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import luti.server.Entity.UrlMapping;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

	// 메서드 남겨놓고 나중에 원본 URL 조회와 디코딩해서 얻은 PK로 조회하는 성능 비교할 때 사용하기 (문자열 조회는 느린 이유가 있다고 한다.)
	UrlMapping findByShortUrl(String s);
}
