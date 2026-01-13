package luti.server.infrastructure.client.kgs;

import java.io.IOException;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResponseErrorHandler;
import luti.server.exception.BusinessException;

import static luti.server.exception.ErrorCode.*;

@Component
public class KgsErrorHandler implements ResponseErrorHandler {

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		// 4xx 또는 5xx 응답이면 true 반환
		return response.getStatusCode().isError();
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		HttpStatusCode statusCode = response.getStatusCode();

		if (statusCode.is5xxServerError()) {
			throw new BusinessException(KGS_UNAVAILABLE);   // 외부 서버 장애
		} else if (statusCode.is4xxClientError()) {
			throw new BusinessException(KGS_INVALID_RESPONSE); // 잘못된 요청 등
		}
	}
}
