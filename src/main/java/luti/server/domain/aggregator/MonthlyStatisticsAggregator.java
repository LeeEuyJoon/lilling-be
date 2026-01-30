package luti.server.domain.aggregator;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import luti.server.domain.model.ClickCountHistory;
import luti.server.domain.service.dto.UrlAnalyticsInfo;

/**
 * 월별 통계 집계자
 * 클릭 히스토리를 월 단위로 집계하여 최근 12개월의 통계를 생성
 */
@Component
public class MonthlyStatisticsAggregator implements StatisticsAggregator<UrlAnalyticsInfo.MonthlyStat> {

	private static final int MONTHLY_RANGE_MONTHS = 12;

	/**
	 * 월별 통계 집계
	 * 현재 월 기준 최근 12개월의 월별 클릭 통계를 생성
	 *
	 * @param histories 클릭 히스토리 목록
	 * @param now 현재 시각
	 * @return 월별 통계 목록 (12개)
	 */
	@Override
	public List<UrlAnalyticsInfo.MonthlyStat> aggregate(List<ClickCountHistory> histories, LocalDateTime now) {

		// 이번 달, since 계산
		YearMonth currentMonth = YearMonth.from(now);
		YearMonth since = currentMonth.minusMonths(MONTHLY_RANGE_MONTHS - 1);

		// since 이후의 클릭 히스토리를 월 단위로 묶어서 Map 생성
		Map<YearMonth, Long> monthlyMap = histories.stream()
												   .filter(h -> !YearMonth.from(h.getHour()).isBefore(since))
												   .collect(Collectors.groupingBy(
													   h -> YearMonth.from(h.getHour()),
													   Collectors.summingLong(ClickCountHistory::getClickCount)
												   ));

		// 빈 데이터 0으로 채우기
		List<UrlAnalyticsInfo.MonthlyStat> result = new ArrayList<>();
		YearMonth current = since;

		while (!current.isAfter(currentMonth)) {
			Long count = monthlyMap.getOrDefault(current, 0L);
			result.add(UrlAnalyticsInfo.MonthlyStat.of(current.toString(), count));
			current = current.plusMonths(1);
		}

		return result;
	}
}
