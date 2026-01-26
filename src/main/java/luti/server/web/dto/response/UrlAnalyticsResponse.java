package luti.server.web.dto.response;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import luti.server.application.result.UrlAnalyticsResult;

public class UrlAnalyticsResponse {

	private TimeSeriesData hourly;
	private TimeSeriesData daily;
	private TimeSeriesData weekly;
	private TimeSeriesData monthly;

	public static UrlAnalyticsResponse from(UrlAnalyticsResult result) {
		UrlAnalyticsResponse response = new UrlAnalyticsResponse();
		response.hourly = TimeSeriesData.fromHourly(result.getHourlyStats());
		response.daily = TimeSeriesData.fromDaily(result.getDailyStats());
		response.weekly = TimeSeriesData.fromWeekly(result.getWeeklyStats());
		response.monthly = TimeSeriesData.fromMonthly(result.getMonthlyStats());
		return response;
	}

	public static class TimeSeriesData {
		private String range;
		private List<?> data;

		public static TimeSeriesData fromHourly(List<UrlAnalyticsResult.HourlyStatResult> stats) {
			TimeSeriesData timeSeriesData = new TimeSeriesData();
			timeSeriesData.range = "24h";
			timeSeriesData.data = stats.stream()
									   .map(stat -> new HourlyDataPoint(
										   stat.getHour().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
										   stat.getClickCount()
									   ))
									   .collect(Collectors.toList());
			return timeSeriesData;
		}

		public static TimeSeriesData fromDaily(List<UrlAnalyticsResult.DailyStatResult> stats) {
			TimeSeriesData timeSeriesData = new TimeSeriesData();
			timeSeriesData.range = "30d";
			timeSeriesData.data = stats.stream()
									   .map(stat -> new DailyDataPoint(
										   stat.getDate().toString(),
										   stat.getClickCount()
									   ))
									   .collect(Collectors.toList());
			return timeSeriesData;
		}

		public static TimeSeriesData fromWeekly(List<UrlAnalyticsResult.WeeklyStatResult> stats) {
			TimeSeriesData timeSeriesData = new TimeSeriesData();
			timeSeriesData.range = "12w";
			timeSeriesData.data = stats.stream()
									   .map(stat -> new WeeklyDataPoint(
										   stat.getWeekStart().toString(),
										   stat.getClickCount()
									   ))
									   .collect(Collectors.toList());
			return timeSeriesData;
		}

		public static TimeSeriesData fromMonthly(List<UrlAnalyticsResult.MonthlyStatResult> stats) {
			TimeSeriesData timeSeriesData = new TimeSeriesData();
			timeSeriesData.range = "12m";
			timeSeriesData.data = stats.stream()
									   .map(stat -> new MonthlyDataPoint(
										   stat.getYearMonth(),
										   stat.getClickCount()
									   ))
									   .collect(Collectors.toList());
			return timeSeriesData;
		}

		public String getRange() {
			return range;
		}

		public List<?> getData() {
			return data;
		}
	}

	public static class HourlyDataPoint {
		private String timestamp;  // ISO 8601: "2025-01-26T00:00:00"
		private Long clickCount;

		public HourlyDataPoint(String timestamp, Long clickCount) {
			this.timestamp = timestamp;
			this.clickCount = clickCount;
		}

		public String getTimestamp() {
			return timestamp;
		}

		public Long getClickCount() {
			return clickCount;
		}
	}

	public static class DailyDataPoint {
		private String date;  // "2025-01-26"
		private Long clickCount;

		public DailyDataPoint(String date, Long clickCount) {
			this.date = date;
			this.clickCount = clickCount;
		}

		public String getDate() {
			return date;
		}

		public Long getClickCount() {
			return clickCount;
		}
	}

	public static class WeeklyDataPoint {
		private String weekStart;  // "2025-01-20"
		private Long clickCount;

		public WeeklyDataPoint(String weekStart, Long clickCount) {
			this.weekStart = weekStart;
			this.clickCount = clickCount;
		}

		public String getWeekStart() {
			return weekStart;
		}

		public Long getClickCount() {
			return clickCount;
		}
	}

	public static class MonthlyDataPoint {
		private String yearMonth;  // "2025-01"
		private Long clickCount;

		public MonthlyDataPoint(String yearMonth, Long clickCount) {
			this.yearMonth = yearMonth;
			this.clickCount = clickCount;
		}

		public String getYearMonth() {
			return yearMonth;
		}

		public Long getClickCount() {
			return clickCount;
		}
	}

	public TimeSeriesData getHourly() {
		return hourly;
	}

	public TimeSeriesData getDaily() {
		return daily;
	}

	public TimeSeriesData getWeekly() {
		return weekly;
	}

	public TimeSeriesData getMonthly() {
		return monthly;
	}
}
