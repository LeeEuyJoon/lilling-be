package luti.server.domain.port;

import java.time.LocalDateTime;
import java.util.List;

import luti.server.domain.model.ClickCountHistory;
import luti.server.domain.model.UrlMapping;

public interface ClickCountHistoryReader {

	List<ClickCountHistory> findByUrlMappingAndHourGreaterThanEqual(
		UrlMapping urlMapping,
		LocalDateTime since
	);

}
