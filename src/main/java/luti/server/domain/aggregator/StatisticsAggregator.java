package luti.server.domain.aggregator;

import java.time.LocalDateTime;
import java.util.List;

import luti.server.domain.model.ClickCountHistory;

/**
 * 통계 집계자 인터페이스
 * 클릭 히스토리 데이터를 특정 시간 단위로 집계하는 책임을 정의
 *
 * @param <T> 집계 결과 타입 (HourlyStat, DailyStat, WeeklyStat, MonthlyStat)
 */
public interface StatisticsAggregator<T> {

	/**
	 * 클릭 히스토리를 집계하여 통계 데이터를 생성
	 *
	 * @param histories 클릭 히스토리 목록
	 * @param now 현재 시각 (집계 기준 시점)
	 * @return 집계된 통계 데이터 목록
	 */
	List<T> aggregate(List<ClickCountHistory> histories, LocalDateTime now);
}
