package luti.server.domain.aggregator;

import static java.time.temporal.ChronoUnit.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import luti.server.domain.model.ClickCountHistory;
import luti.server.domain.service.dto.UrlAnalyticsInfo;

/**
 * 시간별 통계 집계자
 * 클릭 히스토리를 시간 단위로 집계하여 최근 24시간의 통계를 생성
 */
@Component
public class HourlyStatisticsAggregator implements StatisticsAggregator<UrlAnalyticsInfo.HourlyStat> {

	private static final int HOURLY_RANGE_HOURS = 24;

	/**
	 * 시간별 통계 집계
	 * 현재 시각 기준 최근 24시간의 시간별 클릭 통계를 생성
	 *
	 * @param histories 클릭 히스토리 목록
	 * @param now 현재 시각
	 * @return 시간별 통계 목록 (24개)
	 */
	@Override
	public List<UrlAnalyticsInfo.HourlyStat> aggregate(List<ClickCountHistory> histories, LocalDateTime now) {

		// since 계산 (시간별 통계 집계는 현재 시각에서 24시간 전의 데이터부터 집계)
		LocalDateTime since = now.minusHours(HOURLY_RANGE_HOURS - 1).truncatedTo(HOURS);

		// since 이후의 클릭 히스토리를 시간 단위로 묶어서 Map 생성
		Map<LocalDateTime, Long> hourlyMap = histories.stream()
													  .filter(h -> !h.getHour().isBefore(since))
													  .collect(Collectors.groupingBy(
														  ClickCountHistory::getHour,
														  Collectors.summingLong(ClickCountHistory::getClickCount)
													  ));

		// 빈 데이터 0으로 채우기
		List<UrlAnalyticsInfo.HourlyStat> result = new ArrayList<>();
		LocalDateTime current = since;
		LocalDateTime end = now.truncatedTo(HOURS);

		while (!current.isAfter(end)) {
			Long count = hourlyMap.getOrDefault(current, 0L);
			result.add(UrlAnalyticsInfo.HourlyStat.of(current, count));
			current = current.plusHours(1);
		}

		return result;
	}
}
