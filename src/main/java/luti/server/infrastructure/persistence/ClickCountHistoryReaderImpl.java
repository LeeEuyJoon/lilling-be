package luti.server.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import luti.server.domain.model.ClickCountHistory;
import luti.server.domain.model.UrlMapping;
import luti.server.domain.port.ClickCountHistoryReader;

@Component
public class ClickCountHistoryReaderImpl implements ClickCountHistoryReader {

	private final ClickCountHistoryRepository repository;

	public ClickCountHistoryReaderImpl(ClickCountHistoryRepository repository) {
		this.repository = repository;
	}

	@Override
	public List<ClickCountHistory> findByUrlMappingAndHourGreaterThanEqual(UrlMapping urlMapping, LocalDateTime since) {
		return repository.findByUrlMappingAndHourGreaterThanEqualOrderByHourAsc(urlMapping, since);
	}
}
