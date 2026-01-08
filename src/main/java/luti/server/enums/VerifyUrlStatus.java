package luti.server.enums;

public enum VerifyUrlStatus {
	OK,				// 단축 URL 존재하고, 주인도 없음
	NOT_FOUND,		// 단축 URL이 존재하지 않음
	ALREADY_OWNED,	// 단축 URL이 존재하지만, 주인이 있음
	INVALID_FORMAT	// 잘못된 형식의 URL
}
