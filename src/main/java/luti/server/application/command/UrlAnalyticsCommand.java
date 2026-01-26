package luti.server.application.command;

public class UrlAnalyticsCommand {

	private final Long urlMappingId;
	private final Long memberId;

	private UrlAnalyticsCommand(Long urlMappingId, Long memberId) {
		this.urlMappingId = urlMappingId;
		this.memberId = memberId;
	}

	public static UrlAnalyticsCommand of(Long urlMappingId, Long memberId) {
		return new UrlAnalyticsCommand(urlMappingId, memberId);
	}

	public Long getUrlMappingId() {
		return urlMappingId;
	}

	public Long getMemberId() {
		return memberId;
	}
}
