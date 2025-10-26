package luti.server.Client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import luti.server.Client.dto.KeyBlock;

@Component
public class KgsClient {

    private final RestTemplate restTemplate;
    private final String kgsUrl;

    public KgsClient(
        RestTemplate restTemplate,
        @Value("${KGS_URL}") String kgsUrl
    ) {
        this.restTemplate = restTemplate;
        this.kgsUrl = kgsUrl;
    }

    /**
     * KGS 서버로부터 다음 KeyBlock을 요청합니다.
     *
     * @return KeyBlock 객체 (start와 end 범위 포함)
     * @throws RuntimeException KGS 서버 호출 실패 시
     */
    public KeyBlock fetchNextBlock() {
        try {
            String url = kgsUrl + "/api/v1/key/next-block";
            KeyBlock keyBlock = restTemplate.getForObject(url, KeyBlock.class);

            if (keyBlock == null) {
                throw new RuntimeException("KGS returned null KeyBlock");
            }

            return keyBlock;
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to fetch KeyBlock from KGS: " + e.getMessage(), e);
        }
    }
}
