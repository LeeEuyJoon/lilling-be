package luti.server.domain.aggregator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import luti.server.domain.model.ClickCountHistory;
import luti.server.domain.service.dto.UrlAnalyticsInfo;

/**
 * 주별 통계 집계자
 * 클릭 히스토리를 주 단위로 집계하여 최근 12주의 통계를 생성
 */
@Component
public class WeeklyStatisticsAggregator implements StatisticsAggregator<UrlAnalyticsInfo.WeeklyStat> {

	private static final int WEEKLY_RANGE_WEEKS = 12;

	/**
	 * 주별 통계 집계
	 * 현재 주 기준 최근 12주의 주별 클릭 통계를 생성
	 * 주의 시작은 월요일 기준
	 *
	 * @param histories 클릭 히스토리 목록
	 * @param now 현재 시각
	 * @return 주별 통계 목록 (12개)
	 */
	@Override
	public List<UrlAnalyticsInfo.WeeklyStat> aggregate(List<ClickCountHistory> histories, LocalDateTime now) {

		// 이번 주 월요일과 since 계산
		LocalDate currentWeekStart = now.toLocalDate()
										.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate since = currentWeekStart.minusWeeks(WEEKLY_RANGE_WEEKS - 1);

		// since 이후의 클릭 히스토리를 주 단위로 묶어서 Map 생성
		Map<LocalDate, Long> weeklyMap = histories.stream()
												  .filter(h -> !h.getHour().toLocalDate().isBefore(since))
												  .collect(Collectors.groupingBy(
													  h -> h.getHour().toLocalDate()
															.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
													  Collectors.summingLong(ClickCountHistory::getClickCount)
												  ));

		// 빈 데이터 0으로 채우기
		List<UrlAnalyticsInfo.WeeklyStat> result = new ArrayList<>();
		LocalDate current = since;

		while (!current.isAfter(currentWeekStart)) {
			Long count = weeklyMap.getOrDefault(current, 0L);
			result.add(UrlAnalyticsInfo.WeeklyStat.of(current, count));
			current = current.plusWeeks(1);
		}

		return result;
	}
}
