package luti.server.client;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import luti.server.client.dto.KeyBlock;

@Component
public class KeyBlockManager {

	private final KgsClient kgsClient;
	private KeyBlock currentBlock;
	private AtomicLong currentId;

	public KeyBlockManager(KgsClient kgsClient) {
		this.kgsClient = kgsClient;
	}

	public synchronized Long getNextId() {
		// currentId가 null이거나 블록이 소진된 경우 새로운 블록 요청
		if (currentId == null || currentId.get() > currentBlock.getEnd()) {
			currentBlock = kgsClient.fetchNextBlock();
			currentId = new AtomicLong(currentBlock.getStart());
		}
		// 다음 ID 반환
		return currentId.getAndIncrement();
	}

}
