package luti.server.infrastructure.client.kgs;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class KeyBlockManager {

	private static final Logger log = LoggerFactory.getLogger(KeyBlockManager.class);

	private final KgsClient kgsClient;
	private KeyBlock currentBlock;
	private AtomicLong currentId;

	public KeyBlockManager(KgsClient kgsClient) {
		this.kgsClient = kgsClient;
	}

	public synchronized Long getNextId() {
		// currentId가 null이거나 블록이 소진된 경우 새로운 블록 요청
		if (currentId == null || currentId.get() > currentBlock.getEnd()) {
			log.info("KGS 블록 요청: 현재 블록 소진");

			try {
				currentBlock = kgsClient.fetchNextBlock();
				currentId = new AtomicLong(currentBlock.getStart());

				long blockSize = currentBlock.getEnd() - currentBlock.getStart() + 1;
				log.info("KGS 블록 획득 성공: start={}, end={}, size={}",
					currentBlock.getStart(), currentBlock.getEnd(), blockSize);

			} catch (Exception e) {
				log.error("KGS 블록 획득 실패", e);
				throw e;
			}
		}
		// 다음 ID 반환
		return currentId.getAndIncrement();
	}

}
