package luti.server.domain.service;

import static luti.server.exception.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import luti.server.domain.aggregator.DailyStatisticsAggregator;
import luti.server.domain.aggregator.HourlyStatisticsAggregator;
import luti.server.domain.aggregator.MonthlyStatisticsAggregator;
import luti.server.domain.aggregator.WeeklyStatisticsAggregator;
import luti.server.domain.model.ClickCountHistory;
import luti.server.domain.model.UrlMapping;
import luti.server.domain.port.ClickCountHistoryReader;
import luti.server.domain.port.UrlMappingReader;
import luti.server.domain.service.dto.UrlAnalyticsInfo;
import luti.server.domain.validator.UrlOwnershipValidator;
import luti.server.exception.BusinessException;

/**
 * URL 분석 서비스
 * URL의 클릭 통계 데이터를 조회하고 오케스트레이션하는 책임을 가진 서비스
 * 실제 집계 로직은 각 Aggregator에게 위임하여 SRP 준수
 */
@Service
@Transactional(readOnly = true)
public class UrlAnalyticsService {

	private static final int MONTHLY_RANGE_MONTHS = 12;

	private final UrlMappingReader urlMappingReader;
	private final ClickCountHistoryReader clickCountHistoryReader;
	private final UrlOwnershipValidator ownershipValidator;
	private final HourlyStatisticsAggregator hourlyAggregator;
	private final DailyStatisticsAggregator dailyAggregator;
	private final WeeklyStatisticsAggregator weeklyAggregator;
	private final MonthlyStatisticsAggregator monthlyAggregator;

	public UrlAnalyticsService(
		UrlMappingReader urlMappingReader,
		ClickCountHistoryReader clickCountHistoryReader,
		UrlOwnershipValidator ownershipValidator,
		HourlyStatisticsAggregator hourlyAggregator,
		DailyStatisticsAggregator dailyAggregator,
		WeeklyStatisticsAggregator weeklyAggregator,
		MonthlyStatisticsAggregator monthlyAggregator
	) {
		this.urlMappingReader = urlMappingReader;
		this.clickCountHistoryReader = clickCountHistoryReader;
		this.ownershipValidator = ownershipValidator;
		this.hourlyAggregator = hourlyAggregator;
		this.dailyAggregator = dailyAggregator;
		this.weeklyAggregator = weeklyAggregator;
		this.monthlyAggregator = monthlyAggregator;
	}

	/**
	 * URL 분석 정보 조회
	 * 소유권 검증 후 각 시간대별 집계자에게 위임하여 통계 데이터를 생성
	 *
	 * @param urlMappingId URL 매핑 ID
	 * @param memberId 회원 ID
	 * @return UrlAnalyticsInfo 시간대별 통계 데이터
	 */
	public UrlAnalyticsInfo getAnalytics(Long urlMappingId, Long memberId) {

		// URL 조회
		UrlMapping urlMapping = urlMappingReader.findById(urlMappingId)
												.orElseThrow(() -> new BusinessException(URL_NOT_FOUND));

		// 소유권 검증 (위임)
		ownershipValidator.validateOwnership(urlMapping, memberId);

		// 히스토리 조회 (최근 12개월)
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime since = now.minusMonths(MONTHLY_RANGE_MONTHS).withDayOfMonth(1).toLocalDate().atStartOfDay();

		List<ClickCountHistory> histories = clickCountHistoryReader
			.findByUrlMappingAndHourGreaterThanEqual(urlMapping, since);

		// 각 aggregator에게 위임하여 통계 생성
		List<UrlAnalyticsInfo.HourlyStat> hourlyStats = hourlyAggregator.aggregate(histories, now);
		List<UrlAnalyticsInfo.DailyStat> dailyStats = dailyAggregator.aggregate(histories, now);
		List<UrlAnalyticsInfo.WeeklyStat> weeklyStats = weeklyAggregator.aggregate(histories, now);
		List<UrlAnalyticsInfo.MonthlyStat> monthlyStats = monthlyAggregator.aggregate(histories, now);

		return UrlAnalyticsInfo.of(hourlyStats, dailyStats, weeklyStats, monthlyStats);
	}

}
