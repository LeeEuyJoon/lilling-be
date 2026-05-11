package luti.server.infrastructure.security.dto;

public record TokenPair(
	String accessToken,
	long accessTtlSeconds,
	String refreshToken,
	long refreshTtlSeconds
) {}
