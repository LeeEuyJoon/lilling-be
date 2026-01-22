package luti.server.domain.service;

import static luti.server.domain.service.dto.RecentDailyStatisticsInfo.*;
import static luti.server.exception.ErrorCode.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import luti.server.domain.model.ClickCountHistory;
import luti.server.domain.model.UrlMapping;
import luti.server.domain.port.ClickCountHistoryReader;
import luti.server.domain.port.UrlMappingReader;
import luti.server.domain.service.dto.RecentDailyStatisticsInfo;
import luti.server.exception.BusinessException;

@Service
@Transactional(readOnly = true)
public class ClickStatisticsService {

	private final UrlMappingReader urlMappingReader;
	private final ClickCountHistoryReader clickCountHistoryReader;

	public ClickStatisticsService(
		UrlMappingReader urlMappingReader, ClickCountHistoryReader clickCountHistoryReader) {
		this.urlMappingReader = urlMappingReader;
		this.clickCountHistoryReader = clickCountHistoryReader;
	}

	public RecentDailyStatisticsInfo getRecentDailyStatistics(List<Long> urlIds) {
		Map<Long, List<DailyStat>> statisticsMap = new HashMap<>();

		for (Long urlId : urlIds) {
			List<DailyStat> stats = getStatsForSingleUrl(urlId);
			statisticsMap.put(urlId, stats);
		}

		return RecentDailyStatisticsInfo.of(statisticsMap);
	}

	private List<DailyStat> getStatsForSingleUrl(Long urlId) {

		// UrlMapping 조회
		UrlMapping urlMapping = urlMappingReader
			.findById(urlId)
			.orElseThrow(() -> new BusinessException(URL_NOT_FOUND));

		// 최근 7일 계산
		LocalDate today = LocalDate.now();
		LocalDate sevenDaysAgo = today.minusDays(6);
		LocalDateTime since = sevenDaysAgo.atStartOfDay();

		// 히스토리 데이터 조회
		List<ClickCountHistory> histories = clickCountHistoryReader
			.findByUrlMappingAndHourGreaterThanEqual(urlMapping, since);

		// 일별로 집계
		Map<LocalDate, Long> dailyClickMap = new HashMap<>();
		for (ClickCountHistory history: histories) {
			LocalDate date = history.getHour().toLocalDate();
			dailyClickMap.merge(date, history.getClickCount(), Long::sum);
		}

		// 7일치 데이터 생성 및 누락된 날짜 0으로 채우기
		List<DailyStat> result = new ArrayList<>();
		LocalDate currentDate = sevenDaysAgo;

		while (!currentDate.isAfter(today)) {
			Long clickCount = dailyClickMap.getOrDefault(currentDate, 0L);
			result.add(DailyStat.of(currentDate.toString(), clickCount));
			currentDate = currentDate.plusDays(1);
		}

		return result;
	}
}
