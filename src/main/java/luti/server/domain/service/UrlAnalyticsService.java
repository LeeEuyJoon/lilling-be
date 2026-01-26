package luti.server.domain.service;

import static java.time.temporal.ChronoUnit.*;
import static luti.server.exception.ErrorCode.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import luti.server.domain.model.ClickCountHistory;
import luti.server.domain.model.UrlMapping;
import luti.server.domain.port.ClickCountHistoryReader;
import luti.server.domain.port.UrlMappingReader;
import luti.server.domain.service.dto.UrlAnalyticsInfo;
import luti.server.exception.BusinessException;

@Service
@Transactional(readOnly = true)
public class UrlAnalyticsService {

	private static final int HOURLY_RANGE_HOURS = 24;
	private static final int DAILY_RANGE_DAYS = 30;
	private static final int WEEKLY_RANGE_WEEKS = 12;
	private static final int MONTHLY_RANGE_MONTHS = 12;

	private final UrlMappingReader urlMappingReader;
	private final ClickCountHistoryReader clickCountHistoryReader;

	public UrlAnalyticsService(UrlMappingReader urlMappingReader, ClickCountHistoryReader clickCountHistoryReader) {
		this.urlMappingReader = urlMappingReader;
		this.clickCountHistoryReader = clickCountHistoryReader;
	}

	/**
	 * URL 분석 정보 조회
	 * @param urlMappingId
	 * @param memberId
	 * @return UrlAnalyticsInfo
	 */
	public UrlAnalyticsInfo getAnalytics(Long urlMappingId, Long memberId) {

		UrlMapping urlMapping = urlMappingReader.findById(urlMappingId)
												.orElseThrow(() -> new BusinessException(URL_NOT_FOUND));

		if (urlMapping.getMember() == null || !urlMapping.getMember().getId().equals(memberId)) {
			throw new BusinessException(NOT_URL_OWNER);
		}

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime since = now.minusMonths(MONTHLY_RANGE_MONTHS).withDayOfMonth(1).toLocalDate().atStartOfDay();

		List<ClickCountHistory> histories = clickCountHistoryReader
			.findByUrlMappingAndHourGreaterThanEqual(urlMapping, since);

		List<UrlAnalyticsInfo.HourlyStat> hourlyStats = aggregateHourly(histories, now);
		List<UrlAnalyticsInfo.DailyStat> dailyStats = aggregateDaily(histories, now);
		List<UrlAnalyticsInfo.WeeklyStat> weeklyStats = aggregateWeekly(histories, now);
		List<UrlAnalyticsInfo.MonthlyStat> monthlyStats = aggregateMonthly(histories, now);

		return UrlAnalyticsInfo.of(hourlyStats, dailyStats, weeklyStats, monthlyStats);
	}

	/**
	 * 시간별 통계 집계
	 * @param histories
	 * @param now
	 * @return List<UrlAnalyticsInfo.HourlyStat>
	 */
	private List<UrlAnalyticsInfo.HourlyStat> aggregateHourly(List<ClickCountHistory> histories, LocalDateTime now) {

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

	/**
	 * 일별 통계 집계
	 * @param histories
	 * @param now
	 * @return
	 */
	private List<UrlAnalyticsInfo.DailyStat> aggregateDaily(List<ClickCountHistory> histories, LocalDateTime now) {

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

	/**
	 * 주별 통계 집계
	 * @param histories
	 * @param now
	 * @return
	 */
	private List<UrlAnalyticsInfo.WeeklyStat> aggregateWeekly(List<ClickCountHistory> histories, LocalDateTime now) {

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

	/**
	 * 월별 통계 집계
	 * @param histories
	 * @param now
	 * @return
	 */
	private List<UrlAnalyticsInfo.MonthlyStat> aggregateMonthly(List<ClickCountHistory> histories, LocalDateTime now) {

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
