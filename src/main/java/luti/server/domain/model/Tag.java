package luti.server.domain.model;

import static jakarta.persistence.FetchType.*;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
	name = "tag",
	indexes = { @Index(name = "idx_tag_member_id", columnList = "member_id") },
	uniqueConstraints = { @UniqueConstraint(name = "uk_member_tag_name", columnNames = {"member_id", "name"}) }
)
public class Tag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	protected Tag() {}

	private Tag(Builder builder) {
		this.member = builder.member;
		this.name = builder.name;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Long getId() { return id; }
	public Member getMember() { return member; }
	public String getName() { return name; }
	public LocalDateTime getCreatedAt() { return createdAt; }

	public static class Builder {
		private Member member;
		private String name;

		public Builder member(Member member) {
			this.member = member;
			return this;
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Tag build() {
			return new Tag(this);
		}
	}

	public void updateName(String name) {
		this.name = name;
	}

}
