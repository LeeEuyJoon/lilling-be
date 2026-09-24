package luti.server.infrastructure.batch;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import luti.server.infrastructure.batch.dto.ClickCountData;

@Component
public class ClickCountDatabaseWriter implements ItemWriter<ClickCountData> {

	private static final Logger log = LoggerFactory.getLogger(ClickCountDatabaseWriter.class);

	private final JdbcTemplate jdbcTemplate;

	// UPSERT: 같은 (url_mapping_id, hour) 조합이 있으면 click_count 증가, 없으면 INSERT
	private static final String UPSERT_HISTORY_SQL =
		"INSERT INTO click_count_history (url_mapping_id, click_count, hour, created_at, updated_at) " +
		"VALUES (?, ?, ?, NOW(), NOW()) " +
		"ON DUPLICATE KEY UPDATE " +
		"click_count = click_count + VALUES(click_count), " +
		"updated_at = NOW()";

	private static final String UPDATE_TOTAL_SQL =
		"UPDATE url_mapping SET click_count = click_count + ? WHERE scrambled_id = ?";

	public ClickCountDatabaseWriter(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void write(Chunk<? extends ClickCountData> chunk) {
		List<? extends ClickCountData> items = chunk.getItems();

		if (items.isEmpty()) {
			return;
		}

		// scrambled_id -> url_mapping.id 한 번에 조회
		List<Long> scrambledIds = items.stream().map(ClickCountData::getScrambledId).toList();
		Map<Long, Long> urlMappingIdByScrambledId = fetchUrlMappingIds(scrambledIds);

		// url_mapping에 존재하지 않는 scrambledId는 스킵 (삭제된 URL, 잘못된 shortCode 등)
		List<? extends ClickCountData> validItems = items.stream()
				.filter(item -> {
					boolean exists = urlMappingIdByScrambledId.containsKey(item.getScrambledId());
					if (!exists) {
						log.warn("존재하지 않는 scrambledId, 클릭 카운트 동기화 스킵: scrambledId = {}", item.getScrambledId());
					}
					return exists;
					})
				.toList();

		if (validItems.isEmpty()) {
			return;
		}

		// 현재 시간을 시간 단위로 절삭 (분, 초, 나노초 제거)
		LocalDateTime hourTruncated = LocalDateTime.now()
			.withMinute(0)
			.withSecond(0)
			.withNano(0);

		// 히스토리 테이블에 시계열 데이터 UPSERT
		jdbcTemplate.batchUpdate(
			UPSERT_HISTORY_SQL,
			validItems,
			validItems.size(),
			(ps, item) -> {
				ps.setLong(1, urlMappingIdByScrambledId.get(item.getScrambledId())); // 미리 조회한 url_mapping.id
				ps.setLong(2, item.getCount());               // click_count
				ps.setTimestamp(3, Timestamp.valueOf(hourTruncated)); // hour (절삭된 시간)
			}
		);

		// url_mapping 총 클릭 수 업데이트
		jdbcTemplate.batchUpdate(
			UPDATE_TOTAL_SQL,
			validItems,
			validItems.size(),
			(ps, data) -> {
				ps.setLong(1, data.getCount());
				ps.setLong(2, data.getScrambledId());
			}
		);

		log.info("DB에 클릭 수 반영 완료: records={}, skipped={}", validItems.size(), items.size() - validItems.size());
	}

	private Map<Long, Long> fetchUrlMappingIds(List<Long> scrambledIds) {
		String placeholders = scrambledIds.stream().map(id -> "?").collect(Collectors.joining(","));
		String sql = "SELECT id, scrambled_id FROM url_mapping WHERE scrambled_id IN (" + placeholders + ")";

		return jdbcTemplate.query(sql,
				(ResultSetExtractor<Map<Long, Long>>) rs -> {
					Map<Long, Long> result = new HashMap<>();
					while (rs.next()) {
						result.put(rs.getLong("scrambled_id"), rs.getLong("id"));
					}
					return result;
				},
				scrambledIds.toArray()
		);
	}
}
