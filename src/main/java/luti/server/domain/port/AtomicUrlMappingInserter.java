package luti.server.domain.port;

import luti.server.domain.model.UrlMapping;

public interface AtomicUrlMappingInserter {

	boolean tryInsert(UrlMapping urlMapping);
}
