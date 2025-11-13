package luti.server.client.http;

import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Component;
import luti.server.exception.BusinessException;

import static luti.server.exception.ErrorCode.*;

@Component
public class ExternalRestClient {

	private final RestTemplate restTemplate;

	public ExternalRestClient(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public <T> T get(String url, Class<T> type) {
		try {
			return restTemplate.getForObject(url, type);
		} catch (ResourceAccessException e) {
			if (e.getMessage() != null && e.getMessage().contains("timed out")) {
				throw new BusinessException(KGS_TIMEOUT);
			}
			throw new BusinessException(KGS_CONNECTION_FAILED);

		} catch (RestClientException e) {
			// 외에 발생하는 예외들은 비즈니스 의미가 없음 - 로그만 찍고 그대로 전파하는 게 나음
			// 이후에 로깅 작업할 때 처리하기
			throw e;
		}
	}

}
