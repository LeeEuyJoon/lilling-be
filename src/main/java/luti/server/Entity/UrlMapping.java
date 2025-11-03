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

	@Id @GeneratedValue
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@Column(name = "kgs_id", nullable = false, updatable = false)
	private Long kgsId;

	@Column(name = "scrambled_id", nullable = false, updatable = false, unique = true)
	private Long scrambledId;

	@Column(name = "original_url", nullable = false, updatable = false, length = 2048)
	private String originalUrl;

	@Column(name = "short_url", nullable = false, updatable = false, length = 512, unique = true)
	private String shortUrl;

	@Column(name = "app_id", nullable = false, updatable = false)
	private String appId;

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

	public Long getKgsId() {
		return kgsId;
	}

	public Long getScrambledId() {
		return scrambledId;
	}

	public String getShortUrl() {
		return shortUrl;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public String getAppId() {
		return appId;
	}



	// 기본 생성자
	protected UrlMapping() {}

	// Builder

	// Builder용 private 생성자
	private UrlMapping(Builder builder) {
		this.originalUrl = builder.originalUrl;
		this.kgsId = builder.kgsId;
		this.scrambledId = builder.scrambledId;
		this.shortUrl = builder.shortUrl;
		this.appId = builder.appId;
	}

	// static 팩토리 메서드
	public static Builder builder() {
		return new Builder();
	}

	// 내부 Builder 클래스
	public static class Builder {
		private String originalUrl;
		private Long kgsId;
		private Long scrambledId;
		private String shortUrl;
		private String appId;

		public Builder originalUrl(String
			originalUrl) {
			this.originalUrl = originalUrl;
			return this;
		}

		public Builder kgsId(Long kgsId) {
			this.kgsId = kgsId;
			return this;
		}

		public Builder scrambledId(Long scrambledId) {
			this.scrambledId = scrambledId;
			return this;
		}

		public Builder shortUrl(String shortUrl) {
			this.shortUrl = shortUrl;
			return this;
		}

		public Builder appId(String appId) {
			this.appId = appId;
			return this;
		}

		public UrlMapping build() {
			return new UrlMapping(this);
		}

	}
}
