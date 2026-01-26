package luti.server.domain.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class UrlAnalyticsInfo {

	private final List<HourlyStat> hourlyStats;
	private final List<DailyStat> dailyStats;
	private final List<WeeklyStat> weeklyStats;
	private final List<MonthlyStat> monthlyStats;

	private UrlAnalyticsInfo(
			List<HourlyStat> hourlyStats,
			List<DailyStat> dailyStats,
			List<WeeklyStat> weeklyStats,
			List<MonthlyStat> monthlyStats) {
		this.hourlyStats = hourlyStats;
		this.dailyStats = dailyStats;
		this.weeklyStats = weeklyStats;
		this.monthlyStats = monthlyStats;
	}

	public static UrlAnalyticsInfo of(
		List<HourlyStat> hourlyStats,
		List<DailyStat> dailyStats,
		List<WeeklyStat> weeklyStats,
		List<MonthlyStat> monthlyStats) {
		return new UrlAnalyticsInfo(hourlyStats, dailyStats, weeklyStats, monthlyStats);
	}

	public static class HourlyStat {
		private final LocalDateTime hour;
		private final Long clickCount;

		private HourlyStat(LocalDateTime hour, Long clickCount) {
			this.hour = hour;
			this.clickCount = clickCount;
		}

		public static HourlyStat of(LocalDateTime hour, Long clickCount) {
			return new HourlyStat(hour, clickCount);
		}

		public LocalDateTime getHour() { return hour; }
		public Long getClickCount() { return clickCount; }
	}

	public static class DailyStat {
		private final LocalDate date;
		private final Long clickCount;

		private DailyStat(LocalDate date, Long clickCount) {
			this.date = date;
			this.clickCount = clickCount;
		}

		public static DailyStat of(LocalDate date, Long clickCount) {
			return new DailyStat(date, clickCount);
		}

		public LocalDate getDate() { return date; }
		public Long getClickCount() { return clickCount; }
	}

	public static class WeeklyStat {
		private final LocalDate weekStart;
		private final Long clickCount;

		private WeeklyStat(LocalDate weekStart, Long clickCount) {
			this.weekStart = weekStart;
			this.clickCount = clickCount;
		}

		public static WeeklyStat of(LocalDate weekStart, Long clickCount) {
			return new WeeklyStat(weekStart, clickCount);
		}

		public LocalDate getWeekStart() { return weekStart; }
		public Long getClickCount() { return clickCount; }
	}

	public static class MonthlyStat {
		private final String yearMonth;
		private final Long clickCount;

		private MonthlyStat(String yearMonth, Long clickCount) {
			this.yearMonth = yearMonth;
			this.clickCount = clickCount;
		}

		public static MonthlyStat of(String yearMonth, Long clickCount) {
			return new MonthlyStat(yearMonth, clickCount);
		}

		public String getYearMonth() { return yearMonth; }
		public Long getClickCount() { return clickCount; }
	}

	public List<HourlyStat> getHourlyStats() { return hourlyStats; }
	public List<DailyStat> getDailyStats() { return dailyStats; }
	public List<WeeklyStat> getWeeklyStats() { return weeklyStats; }
	public List<MonthlyStat> getMonthlyStats() { return monthlyStats; }

}
