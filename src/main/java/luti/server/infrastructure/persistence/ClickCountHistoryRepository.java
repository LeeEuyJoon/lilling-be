package luti.server.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import luti.server.domain.model.ClickCountHistory;
import luti.server.domain.model.UrlMapping;

public interface ClickCountHistoryRepository extends JpaRepository<ClickCountHistory, Long> {


	/**
	 * URL의 특정 시간 이후 클릭 통계 조회 메서드 (미니 차트용)
	 */
	List<ClickCountHistory> findByUrlMappingAndHourGreaterThanEqualOrderByHourAsc(
		UrlMapping urlMapping,
		LocalDateTime since);
}
