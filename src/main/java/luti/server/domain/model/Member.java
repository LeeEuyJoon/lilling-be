package luti.server.domain.model;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import luti.server.domain.enums.Provider;
import luti.server.domain.enums.Role;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class Member {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, updatable = false)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false)
	private Role role = Role.USER;

	@Enumerated(EnumType.STRING)
	@Column(name = "provider", nullable = false, length = 20)
	private Provider provider;

	@Column(name = "provider_subject", nullable = false, length = 128)
	private String providerSubject;

	@Column(name = "email", length = 255)
	private String email;

	protected Member() {}

	public Member(Provider provider, String providerSubject, String email) {
		this.role = Role.USER;
		this.provider = provider;
		this.providerSubject = providerSubject;
		this.email = email;
	}

	public Long getId() { return id; }
	public Role getRole() { return role; }
	public Provider getProvider() { return provider; }
	public String getProviderSubject() { return providerSubject; }
	public String getEmail() { return email; }

	public void updateEmailIfPresent(String email) {
		if (email != null && !email.isBlank()) {
			this.email = email;
		}
	}
}

