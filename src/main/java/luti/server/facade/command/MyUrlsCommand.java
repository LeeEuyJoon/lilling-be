package luti.server.facade.command;

public class MyUrlsCommand {
	private final Integer page;
	private final Integer size;
	private final Long memberId;

	private MyUrlsCommand(Integer page, Integer size, Long memberId) {
		this.page = page;
		this.size = size;
		this.memberId = memberId;
	}

	public static MyUrlsCommand of(Integer page, Integer size, Long memberId) {
			return new MyUrlsCommand(page, size, memberId);
	}

	public Integer getPage() {
		return page;
	}

	public Integer getSize() {
		return size;
	}

	public Long getMemberId() {
		return memberId;
	}
}
