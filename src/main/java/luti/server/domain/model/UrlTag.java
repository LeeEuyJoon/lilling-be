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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class UrlTag {

	@Id
	@GeneratedValue
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "url_mapping_id", nullable = false, updatable = false)
	private UrlMapping urlMapping;

	@ManyToOne(fetch = LAZY)
	@JoinColumn(name = "tag_id", nullable = false, updatable = false)
	private Tag tag;

	@CreatedDate
	@Column(name = "created_at", updatable = false)
	private LocalDateTime createdAt;

	protected UrlTag() {}

	private UrlTag(UrlMapping urlMapping, Tag tag) {
		this.urlMapping = urlMapping;
		this.tag = tag;
	}

	public static UrlTag of(UrlMapping urlMapping, Tag tag) {
		return new UrlTag(urlMapping, tag);
	}

	public Long getId() { return id; }
	public UrlMapping getUrlMapping() { return urlMapping; }
	public Tag getTag() { return tag; }
	public LocalDateTime getCreatedAt() { return createdAt; }

}
