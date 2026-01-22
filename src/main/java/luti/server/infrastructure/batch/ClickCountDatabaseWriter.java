package luti.server.infrastructure.batch;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import luti.server.infrastructure.batch.dto.ClickCountData;

@Component
public class ClickCountDatabaseWriter implements ItemWriter<ClickCountData> {

	private static final Logger log = LoggerFactory.getLogger(ClickCountDatabaseWriter.class);

	private final JdbcTemplate jdbcTemplate;

	private static final String INSERT_HISTORY_SQL =
		"INSERT INTO click_count_history (scrambled_id, click_count, recorded_at) VALUES (?, ?, ?)";

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

		LocalDateTime now = LocalDateTime.now();

		// 히스토리 테이블에 시계열 데이터 INSERT
		jdbcTemplate.batchUpdate(
			INSERT_HISTORY_SQL,
			items,
			items.size(),
			(ps, item) -> {
				ps.setLong(1, item.getScrambledId());
				ps.setLong(2, item.getCount());
				ps.setTimestamp(3, Timestamp.valueOf(now));
			}
		);

		// url_mapping 총 클릭 수 업데이트
		jdbcTemplate.batchUpdate(
			UPDATE_TOTAL_SQL,
			items,
			items.size(),
			(ps, data) -> {
				ps.setLong(1, data.getCount());
				ps.setLong(2, data.getScrambledId());
			}
		);

		log.info("DB에 클릭 수 반영 완료: records={}", items.size());
	}


}
