package luti.server.infrastructure.security;

import java.util.List;

import org.springframework.stereotype.Service;

import luti.server.exception.BusinessException;
import luti.server.exception.ErrorCode;
import luti.server.infrastructure.security.dto.TokenPair;

@Service
public class AuthTokenService {

	private final JwtProvider jwtProvider;
	private final RefreshTokenStore refreshTokenStore;

	public AuthTokenService(JwtProvider jwtProvider, RefreshTokenStore refreshTokenStore) {
		this.jwtProvider = jwtProvider;
		this.refreshTokenStore = refreshTokenStore;
	}

	public TokenPair rotate(String refreshToken) {
		String jti;
		String memberId;
		List<String> roles;

		try {
			jti = jwtProvider.extractJti(refreshToken);
			memberId = jwtProvider.extractSubject(refreshToken);
			roles = jwtProvider.extractRoles(refreshToken);
		} catch (Exception e) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		if (refreshTokenStore.findMemberId(jti).isEmpty()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}

		refreshTokenStore.delete(jti);

		String newAccessToken = jwtProvider.issueAccessToken(memberId, roles);
		String newRefreshToken = jwtProvider.issueRefreshToken(memberId);
		String newRefreshJti = jwtProvider.extractJti(newRefreshToken);
		refreshTokenStore.save(newRefreshJti, memberId, jwtProvider.getRefreshTtlSeconds());

		return new TokenPair(
			newAccessToken,
			jwtProvider.getAccessTtlSeconds(),
			newRefreshToken,
			jwtProvider.getRefreshTtlSeconds()
		);
	}

	public void revoke(String refreshToken) {
		try {
			String jti = jwtProvider.extractJti(refreshToken);
			refreshTokenStore.delete(jti);
		} catch (Exception ignored) {
			// 만료된 토큰이어도 쿠키 삭제는 진행
		}
	}
}
