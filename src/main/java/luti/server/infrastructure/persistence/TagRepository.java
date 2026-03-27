package luti.server.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import luti.server.domain.model.Tag;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
	List<Tag> findAllByMember_Id(Long memberId);
	Optional<Tag> findByMember_IdAndName(Long memberId, String name);
	long countByMember_Id(Long memberId);
}
