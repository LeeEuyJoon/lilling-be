package luti.server.infrastructure.batch.dto;

public class ClickCountData {
	private final Long scrambledId;
	private final Long count;

	private ClickCountData(Long scrambledId, Long count) {
		this.scrambledId = scrambledId;
		this.count = count;
	}

	public Long getScrambledId() {
		return scrambledId;
	}

	public Long getCount() {
		return count;
	}

	public static ClickCountData of(Long scrambledId, Long count) {
		return new ClickCountData(scrambledId, count);
	}

}
