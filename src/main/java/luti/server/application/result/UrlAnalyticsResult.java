package luti.server.application.result;

import java.time.LocalDateTime;
import java.util.List;

import luti.server.domain.service.dto.UrlAnalyticsInfo;

public class UrlAnalyticsResult {

	private final List<HourlyStatResult> hourlyStats;
	private final List<DailyStatResult> dailyStats;
	private final List<WeeklyStatResult> weeklyStats;
	private final List<MonthlyStatResult> monthlyStats;

	private UrlAnalyticsResult(
		List<HourlyStatResult> hourlyStats,
		List<DailyStatResult> dailyStats,
		List<WeeklyStatResult> weeklyStats,
		List<MonthlyStatResult> monthlyStats
	) {
		this.hourlyStats = hourlyStats;
		this.dailyStats = dailyStats;
		this.weeklyStats = weeklyStats;
		this.monthlyStats = monthlyStats;
	}

	public static UrlAnalyticsResult from(UrlAnalyticsInfo info) {
		return new UrlAnalyticsResult(
			info.getHourlyStats().stream()
				.map(s -> HourlyStatResult.of(s.getHour(), s.getClickCount()))
				.toList(),
			info.getDailyStats().stream()
				.map(s -> DailyStatResult.of(s.getDate().toString(), s.getClickCount()))
				.toList(),
			info.getWeeklyStats().stream()
				.map(s -> WeeklyStatResult.of(s.getWeekStart().toString(), s.getClickCount()))
				.toList(),
			info.getMonthlyStats().stream()
				.map(s -> MonthlyStatResult.of(s.getYearMonth(), s.getClickCount()))
				.toList()
		);
	}

	public static class HourlyStatResult {
		private final LocalDateTime hour;
		private final Long clickCount;

		private HourlyStatResult(LocalDateTime hour, Long clickCount) {
			this.hour = hour;
			this.clickCount = clickCount;
		}

		public static HourlyStatResult of(LocalDateTime hour, Long clickCount) {
			return new HourlyStatResult(hour, clickCount);
		}

		public LocalDateTime getHour() {
			return hour;
		}

		public Long getClickCount() {
			return clickCount;
		}
	}

	public static class DailyStatResult {
		private final String date;
		private final Long clickCount;

		private DailyStatResult(String date, Long clickCount) {
			this.date = date;
			this.clickCount = clickCount;
		}

		public static DailyStatResult of(String date, Long clickCount) {
			return new DailyStatResult(date, clickCount);
		}

		public String getDate() {
			return date;
		}

		public Long getClickCount() {
			return clickCount;
		}
	}

	public static class WeeklyStatResult {
		private final String weekStart;
		private final Long clickCount;

		private WeeklyStatResult(String weekStart, Long clickCount) {
			this.weekStart = weekStart;
			this.clickCount = clickCount;
		}

		public static WeeklyStatResult of(String weekStart, Long clickCount) {
			return new WeeklyStatResult(weekStart, clickCount);
		}

		public String getWeekStart() {
			return weekStart;
		}

		public Long getClickCount() {
			return clickCount;
		}
	}

	public static class MonthlyStatResult {
		private final String yearMonth;
		private final Long clickCount;

		private MonthlyStatResult(String yearMonth, Long clickCount) {
			this.yearMonth = yearMonth;
			this.clickCount = clickCount;
		}

		public static MonthlyStatResult of(String yearMonth, Long clickCount) {
			return new MonthlyStatResult(yearMonth, clickCount);
		}

		public String getYearMonth() {
			return yearMonth;
		}

		public Long getClickCount() {
			return clickCount;
		}
	}

	public List<HourlyStatResult> getHourlyStats() {
		return hourlyStats;
	}

	public List<DailyStatResult> getDailyStats() {
		return dailyStats;
	}

	public List<WeeklyStatResult> getWeeklyStats() {
		return weeklyStats;
	}

	public List<MonthlyStatResult> getMonthlyStats() {
		return monthlyStats;
	}
}
