package luti.server.domain.aggregator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import luti.server.domain.model.ClickCountHistory;
import luti.server.domain.service.dto.UrlAnalyticsInfo;

/**
 * 일별 통계 집계자
 * 클릭 히스토리를 일 단위로 집계하여 최근 30일의 통계를 생성
 */
@Component
public class DailyStatisticsAggregator implements StatisticsAggregator<UrlAnalyticsInfo.DailyStat> {

	private static final int DAILY_RANGE_DAYS = 30;

	/**
	 * 일별 통계 집계
	 * 현재 날짜 기준 최근 30일의 일별 클릭 통계를 생성
	 *
	 * @param histories 클릭 히스토리 목록
	 * @param now 현재 시각
	 * @return 일별 통계 목록 (30개)
	 */
	@Override
	public List<UrlAnalyticsInfo.DailyStat> aggregate(List<ClickCountHistory> histories, LocalDateTime now) {

		// today, since 계산
		LocalDate today = now.toLocalDate();
		LocalDate since = today.minusDays(DAILY_RANGE_DAYS - 1);

		// since 이후의 클릭 히스토리를 일 단위로 묶어서 Map 생성
		Map<LocalDate, Long> dailyMap = histories.stream()
												 .filter(h -> !h.getHour().toLocalDate().isBefore(since))
												 .collect(Collectors.groupingBy(
													 h -> h.getHour().toLocalDate(),
													 Collectors.summingLong(ClickCountHistory::getClickCount)
												 ));

		// 빈 데이터 0으로 채우기
		List<UrlAnalyticsInfo.DailyStat> result = new ArrayList<>();
		LocalDate current = since;

		while (!current.isAfter(today)) {
			Long count = dailyMap.getOrDefault(current, 0L);
			result.add(UrlAnalyticsInfo.DailyStat.of(current, count));
			current = current.plusDays(1);
		}

		return result;
	}
}
