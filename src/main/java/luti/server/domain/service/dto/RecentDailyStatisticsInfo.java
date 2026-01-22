package luti.server.domain.service.dto;

import java.util.List;
import java.util.Map;

public class RecentDailyStatisticsInfo {
	private final Map<Long, List<DailyStat>> statisticsByUrlId;

	private RecentDailyStatisticsInfo(Map<Long, List<DailyStat>> statisticsByUrlId) {
		this.statisticsByUrlId = statisticsByUrlId;
	}

	public static RecentDailyStatisticsInfo of(Map<Long, List<DailyStat>> statisticsByUrlId) {
		return new RecentDailyStatisticsInfo(statisticsByUrlId);
	}

	public List<DailyStat> getStatisticsForUrl(Long urlId) {
		return statisticsByUrlId.getOrDefault(urlId, List.of());
	}

	public static class DailyStat {
		private final String date;
		private final Long clickCount;

		private DailyStat(String date, Long clickCount) {
			this.date = date;
			this.clickCount = clickCount;
		}

		public static DailyStat of(String date, Long clickCount) {
			return new DailyStat(date, clickCount);
		}

		public String getDate() {
			return date;
		}

		public Long getClickCount() {
			return clickCount;
		}

	}

}
