package luti.server.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import luti.server.domain.aggregator.DailyStatisticsAggregator;
import luti.server.domain.aggregator.HourlyStatisticsAggregator;
import luti.server.domain.aggregator.MonthlyStatisticsAggregator;
import luti.server.domain.aggregator.WeeklyStatisticsAggregator;
import luti.server.domain.enums.Provider;
import luti.server.domain.model.ClickCountHistory;
import luti.server.domain.model.Member;
import luti.server.domain.model.UrlMapping;
import luti.server.domain.port.ClickCountHistoryReader;
import luti.server.domain.port.UrlMappingReader;
import luti.server.domain.service.dto.UrlAnalyticsInfo;
import luti.server.domain.validator.UrlOwnershipValidator;
import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class UrlAnalyticsServiceTest {

	@Mock
	private UrlMappingReader urlMappingReader;

	@Mock
	private ClickCountHistoryReader clickCountHistoryReader;

	private UrlAnalyticsService urlAnalyticsService;

	private Member testMember;
	private UrlMapping testUrlMapping;

	@BeforeEach
	void setUp() {
		testMember = new Member(Provider.GOOGLE, "google-test", "test@example.com");
		// Use reflection to set ID since Member doesn't have setter
		setMemberId(testMember, 1L);

		testUrlMapping = UrlMapping.builder()
			.kgsId(1000L)
			.scrambledId(12345L)
			.originalUrl("https://example.com")
			.shortUrl("lill.ing/abc123")
			.appId("test-app")
			.member(testMember)
			.clickCount(100L)
			.build();
		setUrlMappingId(testUrlMapping, 1L);

		// 실제 인스턴스 생성 (검증 로직과 집계 로직을 테스트하기 위해)
		UrlOwnershipValidator ownershipValidator = new UrlOwnershipValidator();
		HourlyStatisticsAggregator hourlyAggregator = new HourlyStatisticsAggregator();
		DailyStatisticsAggregator dailyAggregator = new DailyStatisticsAggregator();
		WeeklyStatisticsAggregator weeklyAggregator = new WeeklyStatisticsAggregator();
		MonthlyStatisticsAggregator monthlyAggregator = new MonthlyStatisticsAggregator();

		// UrlAnalyticsService 생성 (실제 인스턴스 주입)
		urlAnalyticsService = new UrlAnalyticsService(
			urlMappingReader,
			clickCountHistoryReader,
			ownershipValidator,
			hourlyAggregator,
			dailyAggregator,
			weeklyAggregator,
			monthlyAggregator
		);
	}

	// Helper method to set private ID field via reflection
	private void setMemberId(Member member, Long id) {
		try {
			var field = Member.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(member, id);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void setUrlMappingId(UrlMapping urlMapping, Long id) {
		try {
			var field = UrlMapping.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(urlMapping, id);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	@DisplayName("URL 통계 조회 - 정상 동작 (데이터 없음)")
	void getAnalytics_정상동작_데이터없음() {
		// Given
		Long urlMappingId = 1L;
		Long memberId = 1L;

		when(urlMappingReader.findById(urlMappingId)).thenReturn(Optional.of(testUrlMapping));
		when(clickCountHistoryReader.findByUrlMappingAndHourGreaterThanEqual(eq(testUrlMapping), any(LocalDateTime.class)))
			.thenReturn(new ArrayList<>());

		System.out.println("=== URL 통계 조회 테스트 (데이터 없음) ===");
		System.out.println("URL Mapping ID: " + urlMappingId);
		System.out.println("Member ID: " + memberId);

		// When
		UrlAnalyticsInfo result = urlAnalyticsService.getAnalytics(urlMappingId, memberId);

		// Then
		assertNotNull(result);

		// 시간별 통계: 24시간 = 24개
		assertEquals(24, result.getHourlyStats().size());
		assertTrue(result.getHourlyStats().stream().allMatch(stat -> stat.getClickCount() == 0L));

		// 일별 통계: 30일 = 30개
		assertEquals(30, result.getDailyStats().size());
		assertTrue(result.getDailyStats().stream().allMatch(stat -> stat.getClickCount() == 0L));

		// 주별 통계: 12주 = 12개
		assertEquals(12, result.getWeeklyStats().size());
		assertTrue(result.getWeeklyStats().stream().allMatch(stat -> stat.getClickCount() == 0L));

		// 월별 통계: 12개월 = 12개
		assertEquals(12, result.getMonthlyStats().size());
		assertTrue(result.getMonthlyStats().stream().allMatch(stat -> stat.getClickCount() == 0L));

		System.out.println("시간별 통계 개수: " + result.getHourlyStats().size());
		System.out.println("일별 통계 개수: " + result.getDailyStats().size());
		System.out.println("주별 통계 개수: " + result.getWeeklyStats().size());
		System.out.println("월별 통계 개수: " + result.getMonthlyStats().size());
		System.out.println("모든 통계 값이 0으로 채워짐을 확인");

		verify(urlMappingReader).findById(urlMappingId);
		verify(clickCountHistoryReader).findByUrlMappingAndHourGreaterThanEqual(eq(testUrlMapping), any(LocalDateTime.class));
	}

	@Test
	@DisplayName("URL 통계 조회 - 정상 동작 (시간별 데이터)")
	void getAnalytics_정상동작_시간별데이터() {
		// Given
		Long urlMappingId = 1L;
		Long memberId = 1L;

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime since = now.minusMonths(12).withDayOfMonth(1).toLocalDate().atStartOfDay();

		// 최근 24시간 내 데이터 생성
		List<ClickCountHistory> histories = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			LocalDateTime hour = now.minusHours(i).truncatedTo(java.time.temporal.ChronoUnit.HOURS);
			ClickCountHistory history = createClickCountHistory(testUrlMapping, hour, 10L);
			histories.add(history);
		}

		when(urlMappingReader.findById(urlMappingId)).thenReturn(Optional.of(testUrlMapping));
		when(clickCountHistoryReader.findByUrlMappingAndHourGreaterThanEqual(testUrlMapping, since))
			.thenReturn(histories);

		System.out.println("=== URL 통계 조회 테스트 (시간별 데이터) ===");
		System.out.println("생성된 히스토리 개수: " + histories.size());

		// When
		UrlAnalyticsInfo result = urlAnalyticsService.getAnalytics(urlMappingId, memberId);

		// Then
		assertNotNull(result);
		assertEquals(24, result.getHourlyStats().size());

		// 최근 5시간은 클릭 카운트가 10
		long nonZeroCount = result.getHourlyStats().stream()
			.filter(stat -> stat.getClickCount() > 0)
			.count();

		assertEquals(5, nonZeroCount);

		System.out.println("시간별 통계 - 0이 아닌 값 개수: " + nonZeroCount);
		System.out.println("시간별 통계 첫 5개 값:");
		for (int i = 0; i < Math.min(5, result.getHourlyStats().size()); i++) {
			var stat = result.getHourlyStats().get(i);
			System.out.println("  " + stat.getHour() + " -> " + stat.getClickCount());
		}
	}

	@Test
	@DisplayName("URL 통계 조회 - 정상 동작 (일별 데이터)")
	void getAnalytics_정상동작_일별데이터() {
		// Given
		Long urlMappingId = 1L;
		Long memberId = 1L;

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime since = now.minusMonths(12).withDayOfMonth(1).toLocalDate().atStartOfDay();

		// 최근 7일 데이터 생성
		List<ClickCountHistory> histories = new ArrayList<>();
		for (int i = 0; i < 7; i++) {
			LocalDate date = now.toLocalDate().minusDays(i);
			// 하루에 여러 시간 데이터 생성
			for (int hour = 0; hour < 24; hour += 6) {
				LocalDateTime time = date.atStartOfDay().plusHours(hour);
				ClickCountHistory history = createClickCountHistory(testUrlMapping, time, 5L);
				histories.add(history);
			}
		}

		when(urlMappingReader.findById(urlMappingId)).thenReturn(Optional.of(testUrlMapping));
		when(clickCountHistoryReader.findByUrlMappingAndHourGreaterThanEqual(testUrlMapping, since))
			.thenReturn(histories);

		System.out.println("=== URL 통계 조회 테스트 (일별 데이터) ===");
		System.out.println("생성된 히스토리 개수: " + histories.size());

		// When
		UrlAnalyticsInfo result = urlAnalyticsService.getAnalytics(urlMappingId, memberId);

		// Then
		assertNotNull(result);
		assertEquals(30, result.getDailyStats().size());

		// 최근 7일은 각각 20 (4시간 * 5)
		long nonZeroCount = result.getDailyStats().stream()
			.filter(stat -> stat.getClickCount() > 0)
			.count();

		assertEquals(7, nonZeroCount);

		System.out.println("일별 통계 - 0이 아닌 값 개수: " + nonZeroCount);
		System.out.println("일별 통계 최근 7일:");
		for (int i = 0; i < Math.min(7, result.getDailyStats().size()); i++) {
			var stat = result.getDailyStats().get(result.getDailyStats().size() - 1 - i);
			System.out.println("  " + stat.getDate() + " -> " + stat.getClickCount());
		}
	}

	@Test
	@DisplayName("URL 통계 조회 - 정상 동작 (주별 데이터)")
	void getAnalytics_정상동작_주별데이터() {
		// Given
		Long urlMappingId = 1L;
		Long memberId = 1L;

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime since = now.minusMonths(12).withDayOfMonth(1).toLocalDate().atStartOfDay();

		// 최근 4주 데이터 생성
		List<ClickCountHistory> histories = new ArrayList<>();
		for (int week = 0; week < 4; week++) {
			LocalDate weekStart = now.toLocalDate()
				.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
				.minusWeeks(week);

			// 각 주의 월요일에 데이터 추가
			LocalDateTime time = weekStart.atStartOfDay();
			ClickCountHistory history = createClickCountHistory(testUrlMapping, time, 50L);
			histories.add(history);
		}

		when(urlMappingReader.findById(urlMappingId)).thenReturn(Optional.of(testUrlMapping));
		when(clickCountHistoryReader.findByUrlMappingAndHourGreaterThanEqual(testUrlMapping, since))
			.thenReturn(histories);

		System.out.println("=== URL 통계 조회 테스트 (주별 데이터) ===");
		System.out.println("생성된 히스토리 개수: " + histories.size());

		// When
		UrlAnalyticsInfo result = urlAnalyticsService.getAnalytics(urlMappingId, memberId);

		// Then
		assertNotNull(result);
		assertEquals(12, result.getWeeklyStats().size());

		long nonZeroCount = result.getWeeklyStats().stream()
			.filter(stat -> stat.getClickCount() > 0)
			.count();

		assertEquals(4, nonZeroCount);

		System.out.println("주별 통계 - 0이 아닌 값 개수: " + nonZeroCount);
		System.out.println("주별 통계 최근 4주:");
		for (int i = 0; i < Math.min(4, result.getWeeklyStats().size()); i++) {
			var stat = result.getWeeklyStats().get(result.getWeeklyStats().size() - 1 - i);
			System.out.println("  " + stat.getWeekStart() + " -> " + stat.getClickCount());
		}
	}

	@Test
	@DisplayName("URL 통계 조회 - 정상 동작 (월별 데이터)")
	void getAnalytics_정상동작_월별데이터() {
		// Given
		Long urlMappingId = 1L;
		Long memberId = 1L;

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime since = now.minusMonths(12).withDayOfMonth(1).toLocalDate().atStartOfDay();

		// 최근 6개월 데이터 생성
		List<ClickCountHistory> histories = new ArrayList<>();
		for (int month = 0; month < 6; month++) {
			YearMonth targetMonth = YearMonth.from(now).minusMonths(month);
			LocalDateTime time = targetMonth.atDay(1).atStartOfDay();

			ClickCountHistory history = createClickCountHistory(testUrlMapping, time, 100L);
			histories.add(history);
		}

		when(urlMappingReader.findById(urlMappingId)).thenReturn(Optional.of(testUrlMapping));
		when(clickCountHistoryReader.findByUrlMappingAndHourGreaterThanEqual(testUrlMapping, since))
			.thenReturn(histories);

		System.out.println("=== URL 통계 조회 테스트 (월별 데이터) ===");
		System.out.println("생성된 히스토리 개수: " + histories.size());

		// When
		UrlAnalyticsInfo result = urlAnalyticsService.getAnalytics(urlMappingId, memberId);

		// Then
		assertNotNull(result);
		assertEquals(12, result.getMonthlyStats().size());

		long nonZeroCount = result.getMonthlyStats().stream()
			.filter(stat -> stat.getClickCount() > 0)
			.count();

		assertEquals(6, nonZeroCount);

		System.out.println("월별 통계 - 0이 아닌 값 개수: " + nonZeroCount);
		System.out.println("월별 통계 최근 6개월:");
		for (int i = 0; i < Math.min(6, result.getMonthlyStats().size()); i++) {
			var stat = result.getMonthlyStats().get(result.getMonthlyStats().size() - 1 - i);
			System.out.println("  " + stat.getYearMonth() + " -> " + stat.getClickCount());
		}
	}

	@Test
	@DisplayName("URL 통계 조회 - 존재하지 않는 URL")
	void getAnalytics_존재하지않는URL() {
		// Given
		Long urlMappingId = 999L;
		Long memberId = 1L;

		when(urlMappingReader.findById(urlMappingId)).thenReturn(Optional.empty());

		System.out.println("=== URL 통계 조회 테스트 (존재하지 않는 URL) ===");
		System.out.println("URL Mapping ID: " + urlMappingId);

		// When & Then
		BusinessException exception = assertThrows(BusinessException.class,
			() -> urlAnalyticsService.getAnalytics(urlMappingId, memberId));

		assertEquals(ErrorCode.URL_NOT_FOUND, exception.getErrorCode());
		System.out.println("예외 발생: " + exception.getMessage());

		verify(urlMappingReader).findById(urlMappingId);
		verify(clickCountHistoryReader, never()).findByUrlMappingAndHourGreaterThanEqual(any(), any());
	}

	@Test
	@DisplayName("URL 통계 조회 - 소유자가 아닌 사용자")
	void getAnalytics_소유자가아님() {
		// Given
		Long urlMappingId = 1L;
		Long differentMemberId = 999L;

		when(urlMappingReader.findById(urlMappingId)).thenReturn(Optional.of(testUrlMapping));

		System.out.println("=== URL 통계 조회 테스트 (소유자가 아님) ===");
		System.out.println("URL 소유자 ID: " + testMember.getId());
		System.out.println("요청자 ID: " + differentMemberId);

		// When & Then
		BusinessException exception = assertThrows(BusinessException.class,
			() -> urlAnalyticsService.getAnalytics(urlMappingId, differentMemberId));

		assertEquals(ErrorCode.NOT_URL_OWNER, exception.getErrorCode());
		System.out.println("예외 발생: " + exception.getMessage());

		verify(urlMappingReader).findById(urlMappingId);
		verify(clickCountHistoryReader, never()).findByUrlMappingAndHourGreaterThanEqual(any(), any());
	}

	@Test
	@DisplayName("URL 통계 조회 - 소유자가 null인 URL")
	void getAnalytics_소유자Null() {
		// Given
		Long urlMappingId = 1L;
		Long memberId = 1L;

		UrlMapping urlWithoutOwner = UrlMapping.builder()
			.kgsId(1000L)
			.scrambledId(12345L)
			.originalUrl("https://example.com")
			.shortUrl("lill.ing/abc123")
			.appId("test-app")
			.member(null)  // 소유자 없음
			.clickCount(0L)
			.build();

		when(urlMappingReader.findById(urlMappingId)).thenReturn(Optional.of(urlWithoutOwner));

		System.out.println("=== URL 통계 조회 테스트 (소유자 없는 URL) ===");
		System.out.println("URL 소유자: null");
		System.out.println("요청자 ID: " + memberId);

		// When & Then
		BusinessException exception = assertThrows(BusinessException.class,
			() -> urlAnalyticsService.getAnalytics(urlMappingId, memberId));

		assertEquals(ErrorCode.NOT_URL_OWNER, exception.getErrorCode());
		System.out.println("예외 발생: " + exception.getMessage());

		verify(urlMappingReader).findById(urlMappingId);
		verify(clickCountHistoryReader, never()).findByUrlMappingAndHourGreaterThanEqual(any(), any());
	}

	@Test
	@DisplayName("URL 통계 조회 - 복합 데이터 (모든 기간)")
	void getAnalytics_복합데이터() {
		// Given
		Long urlMappingId = 1L;
		Long memberId = 1L;

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime since = now.minusMonths(12).withDayOfMonth(1).toLocalDate().atStartOfDay();

		List<ClickCountHistory> histories = new ArrayList<>();

		// 최근 12시간 데이터
		for (int i = 0; i < 12; i++) {
			LocalDateTime hour = now.minusHours(i).truncatedTo(java.time.temporal.ChronoUnit.HOURS);
			histories.add(createClickCountHistory(testUrlMapping, hour, 5L));
		}

		// 최근 15일 데이터 (하루에 한 번)
		for (int i = 0; i < 15; i++) {
			LocalDate date = now.toLocalDate().minusDays(i);
			histories.add(createClickCountHistory(testUrlMapping, date.atTime(12, 0), 20L));
		}

		// 최근 8주 데이터
		for (int week = 0; week < 8; week++) {
			LocalDate weekStart = now.toLocalDate()
				.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
				.minusWeeks(week);
			histories.add(createClickCountHistory(testUrlMapping, weekStart.atStartOfDay(), 100L));
		}

		// 최근 10개월 데이터
		for (int month = 0; month < 10; month++) {
			YearMonth targetMonth = YearMonth.from(now).minusMonths(month);
			histories.add(createClickCountHistory(testUrlMapping, targetMonth.atDay(1).atStartOfDay(), 500L));
		}

		when(urlMappingReader.findById(urlMappingId)).thenReturn(Optional.of(testUrlMapping));
		when(clickCountHistoryReader.findByUrlMappingAndHourGreaterThanEqual(testUrlMapping, since))
			.thenReturn(histories);

		System.out.println("=== URL 통계 조회 테스트 (복합 데이터) ===");
		System.out.println("생성된 총 히스토리 개수: " + histories.size());

		// When
		UrlAnalyticsInfo result = urlAnalyticsService.getAnalytics(urlMappingId, memberId);

		// Then
		assertNotNull(result);

		// 모든 통계가 올바른 크기를 가지는지 확인
		assertEquals(24, result.getHourlyStats().size());
		assertEquals(30, result.getDailyStats().size());
		assertEquals(12, result.getWeeklyStats().size());
		assertEquals(12, result.getMonthlyStats().size());

		// 각 통계에 0이 아닌 값이 존재하는지 확인
		assertTrue(result.getHourlyStats().stream().anyMatch(stat -> stat.getClickCount() > 0));
		assertTrue(result.getDailyStats().stream().anyMatch(stat -> stat.getClickCount() > 0));
		assertTrue(result.getWeeklyStats().stream().anyMatch(stat -> stat.getClickCount() > 0));
		assertTrue(result.getMonthlyStats().stream().anyMatch(stat -> stat.getClickCount() > 0));

		System.out.println("시간별 통계 - 0이 아닌 값: " +
			result.getHourlyStats().stream().filter(s -> s.getClickCount() > 0).count());
		System.out.println("일별 통계 - 0이 아닌 값: " +
			result.getDailyStats().stream().filter(s -> s.getClickCount() > 0).count());
		System.out.println("주별 통계 - 0이 아닌 값: " +
			result.getWeeklyStats().stream().filter(s -> s.getClickCount() > 0).count());
		System.out.println("월별 통계 - 0이 아닌 값: " +
			result.getMonthlyStats().stream().filter(s -> s.getClickCount() > 0).count());
	}

	// Helper method to create ClickCountHistory with Builder
	private ClickCountHistory createClickCountHistory(UrlMapping urlMapping, LocalDateTime hour, Long clickCount) {
		ClickCountHistory history = ClickCountHistory.builder()
			.urlMapping(urlMapping)
			.hour(hour)
			.build();

		// Set clickCount using reflection since there's no setter
		try {
			var field = ClickCountHistory.class.getDeclaredField("clickCount");
			field.setAccessible(true);
			field.set(history, clickCount);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return history;
	}
}
