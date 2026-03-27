package luti.server.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import luti.server.domain.model.Tag;
import luti.server.domain.model.UrlTag;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
	List<Tag> findAllByMember_Id(Long memberId);
	List<UrlTag> findByUrlMapping_IdIn(List<Long> urlMappingIds);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM UrlTag ut WHERE ut.urlMapping.id = :urlMappingId AND ut.tag.id IN :TagIds")
	void deleteByUrlMappingIdAndTagIdIn(@Param("urlMappingId") Long urlMappingId, @Param("tagIds") List<Long> tagIds);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM UrlTag ut WHERE ut.tag.id = :tagId")
	void deleteByTagId(@Param("tagId") Long tagId);
}
