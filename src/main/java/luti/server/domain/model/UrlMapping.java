package luti.server.domain.model;

import static jakarta.persistence.FetchType.*;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "url_mapping",
	indexes = {
		@Index(name = "idx_member_id", columnList = "member_id"),
	}
)
public class UrlMapping {

	// 필드

	@Id
	@GeneratedValue
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@Column(name = "kgs_id", nullable = false, updatable = false)
	private Long kgsId;

	@Column(name = "scrambled_id", nullable = true, updatable = false, unique = true)
	private Long scrambledId;

	@Column(name = "original_url", nullable = false, updatable = false, length = 2048)
	private String originalUrl;

	@Column(name = "short_url", nullable = false, updatable = false, length = 512, unique = true)
	private String shortUrl;

	@Column(name = "app_id", nullable = false, updatable = false)
	private String appId;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "member_id", nullable = true, updatable = true)
	private Member member;

	@CreatedDate
	@Column(updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "click_count", nullable = false, updatable = true)
	private Long clickCount;

	@Column(name = "description", nullable = true, updatable = true)
	private String description;

	@Column(name = "is_deleted", nullable = true, updatable = true)
	private Boolean isDeleted = false;

	@Column(name = "deleted_at", nullable = true, updatable = true)
	private LocalDateTime deletedAt = null;

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

	public Member getMember() {
		return member;
	}

	public Long getClickCount() {
		return clickCount;
	}

	public String getDescription() {
		return description;
	}

	public Boolean getDeleted() { return isDeleted; }

	public LocalDateTime getDeletedAt() { return deletedAt; }

	// 생성자
	protected UrlMapping() {
	}

	private UrlMapping(Builder builder) {
		this.originalUrl = builder.originalUrl;
		this.kgsId = builder.kgsId;
		this.scrambledId = builder.scrambledId;
		this.shortUrl = builder.shortUrl;
		this.appId = builder.appId;
		this.member = builder.member;
		this.clickCount = builder.clickCount;
		this.description = builder.description;
	}

	// Builder

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
		private Member member;
		private Long clickCount = 0L;
		private String description;

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

		public Builder member(Member member) {
			this.member = member;
			return this;
		}

		public Builder clickCount(Long clickCount) {
			this.clickCount = clickCount;
			return this;
		}

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		public UrlMapping build() {
			return new UrlMapping(this);
		}

	}
}
