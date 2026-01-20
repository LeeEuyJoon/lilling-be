package luti.server.domain.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "click_count_history",
	indexes = @Index(name = "idx_url_mapping_hour", columnList = "url_mapping_id, hour"),
	uniqueConstraints = @UniqueConstraint(
		name = "uk_url_mapping_hour",
		columnNames = {"url_mapping_id", "hour"}
	)
)
public class ClickCountHistory {

	// 필드

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "url_mapping_id", nullable = false)
	private UrlMapping urlMapping;

	@Column(name = "hour", nullable = false)
	private LocalDateTime hour;  // 시간 단위로 절삭 (2026-01-21 03:00:00)

	@Column(name = "click_count", nullable = false)
	private Long clickCount = 0L;

	@CreatedDate
	@Column(updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	// 생성자

	protected ClickCountHistory() {}

	private ClickCountHistory(Builder builder) {
		this.urlMapping = builder.urlMapping;
		this.hour = builder.hour;
		this.clickCount = 0L;
	}

	// Builder

	// static 팩토리 메서드
	public static Builder builder() {
		return new Builder();
	}

	// 내부 Builder 클래스
	public static class Builder {
		private UrlMapping urlMapping;
		private LocalDateTime hour;

		public Builder urlMapping(UrlMapping urlMapping) {
			this.urlMapping = urlMapping;
			return this;
		}

		public Builder hour(LocalDateTime hour) {
			this.hour = hour;
			return this;
		}

		public ClickCountHistory build() {
			return new ClickCountHistory(this);
		}
	}

	public Long getId() {
		return id;
	}

	public UrlMapping getUrlMapping() {
		return urlMapping;
	}

	public LocalDateTime getHour() {
		return hour;
	}

	public Long getClickCount() {
		return clickCount;
	}
}
