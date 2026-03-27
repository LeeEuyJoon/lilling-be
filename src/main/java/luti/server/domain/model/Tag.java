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
import jakarta.persistence.ManyToOne;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class Tag {

	@Id
	@GeneratedValue
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = LAZY)
	@Column(name = "member_id", nullable = false, updatable = false)
	private Member member;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@CreatedDate
	@Column(name = "color", nullable = false)
	private LocalDateTime createdAt;

	protected Tag() {}

	private Tag(Builder builder) {
		this.member = builder.member;
		this.name = builder.name;
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
