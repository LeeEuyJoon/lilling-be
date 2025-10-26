package luti.server.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import luti.server.Entity.UrlMapping;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

	UrlMapping findByShortUrl(String s);
}
