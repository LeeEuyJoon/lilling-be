package luti.server.application.result;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import luti.server.domain.service.dto.UrlAnalyticsInfo;

public class UrlAnalyticsResult {

	@JsonProperty("hourly")
	private final TimeSeriesResult<HourlyStatResult> hourly;

	@JsonProperty("daily")
	private final TimeSeriesResult<DailyStatResult> daily;

	@JsonProperty("weekly")
	private final TimeSeriesResult<WeeklyStatResult> weekly;

	@JsonProperty("monthly")
	private final TimeSeriesResult<MonthlyStatResult> monthly;

	private UrlAnalyticsResult(
		TimeSeriesResult<HourlyStatResult> hourly,
		TimeSeriesResult<DailyStatResult> daily,
		TimeSeriesResult<WeeklyStatResult> weekly,
		TimeSeriesResult<MonthlyStatResult> monthly
	) {
		this.hourly = hourly;
		this.daily = daily;
		this.weekly = weekly;
		this.monthly = monthly;
	}

	public static UrlAnalyticsResult from(UrlAnalyticsInfo info) {
		List<HourlyStatResult> hourlyData = info.getHourlyStats().stream()
			.map(s -> HourlyStatResult.of(s.getHour(), s.getClickCount()))
			.toList();
		List<DailyStatResult> dailyData = info.getDailyStats().stream()
			.map(s -> DailyStatResult.of(s.getDate().toString(), s.getClickCount()))
			.toList();
		List<WeeklyStatResult> weeklyData = info.getWeeklyStats().stream()
			.map(s -> WeeklyStatResult.of(s.getWeekStart().toString(), s.getClickCount()))
			.toList();
		List<MonthlyStatResult> monthlyData = info.getMonthlyStats().stream()
			.map(s -> MonthlyStatResult.of(s.getYearMonth(), s.getClickCount()))
			.toList();

		return new UrlAnalyticsResult(
			TimeSeriesResult.of("24h", hourlyData),
			TimeSeriesResult.of("30d", dailyData),
			TimeSeriesResult.of("12w", weeklyData),
			TimeSeriesResult.of("12m", monthlyData)
		);
	}

	public static class TimeSeriesResult<T> {
		@JsonProperty("range")
		private final String range;

		@JsonProperty("data")
		private final List<T> data;

		private TimeSeriesResult(String range, List<T> data) {
			this.range = range;
			this.data = data;
		}

		public static <T> TimeSeriesResult<T> of(String range, List<T> data) {
			return new TimeSeriesResult<>(range, data);
		}

		public String getRange() {
			return range;
		}

		public List<T> getData() {
			return data;
		}
	}

	public static class HourlyStatResult {
		@JsonProperty("timestamp")
		private final LocalDateTime timestamp;

		@JsonProperty("clickCount")
		private final Long clickCount;

		private HourlyStatResult(LocalDateTime timestamp, Long clickCount) {
			this.timestamp = timestamp;
			this.clickCount = clickCount;
		}

		public static HourlyStatResult of(LocalDateTime timestamp, Long clickCount) {
			return new HourlyStatResult(timestamp, clickCount);
		}

		public LocalDateTime getTimestamp() {
			return timestamp;
		}

		public Long getClickCount() {
			return clickCount;
		}
	}

	public static class DailyStatResult {
		@JsonProperty("date")
		private final String date;

		@JsonProperty("clickCount")
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
		@JsonProperty("weekStart")
		private final String weekStart;

		@JsonProperty("clickCount")
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
		@JsonProperty("yearMonth")
		private final String yearMonth;

		@JsonProperty("clickCount")
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

	public TimeSeriesResult<HourlyStatResult> getHourly() {
		return hourly;
	}

	public TimeSeriesResult<DailyStatResult> getDaily() {
		return daily;
	}

	public TimeSeriesResult<WeeklyStatResult> getWeekly() {
		return weekly;
	}

	public TimeSeriesResult<MonthlyStatResult> getMonthly() {
		return monthly;
	}
}
