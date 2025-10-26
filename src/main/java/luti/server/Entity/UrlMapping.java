package luti.server.Entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class UrlMapping {

	// 필드

	@Id
	@GeneratedValue
	private Long id;

	private String originalUrl;

	private Long scrambleId;

	private String shortUrl;

	@CreatedDate
	@Column(updatable = false)
	private LocalDateTime createdAt;

	// Getter 메서드

	public Long getId() {
		return id;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public Long getScrambleId() {
		return scrambleId;
	}

	public String getShortUrl() {
		return shortUrl;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	// 기본 생성자
	protected UrlMapping() {}

	// Builder

	// Builder용 private 생성자
	private UrlMapping(Builder builder) {
		this.originalUrl = builder.originalUrl;
		this.scrambleId = builder.scrambleId;
		this.shortUrl = builder.shortUrl;
	}

	// static 팩토리 메서드
	public static Builder builder() {
		return new Builder();
	}

	// 내부 Builder 클래스
	public static class Builder {
		private String originalUrl;
		private Long scrambleId;
		private String shortUrl;

		public Builder originalUrl(String
			originalUrl) {
			this.originalUrl = originalUrl;
			return this;
		}

		public Builder scrambleId(Long
			scrambleId) {
			this.scrambleId = scrambleId;
			return this;
		}

		public Builder shortUrl(String shortUrl) {
			this.shortUrl = shortUrl;
			return this;
		}

		public UrlMapping build() {
			return new UrlMapping(this);
		}
	}
}
