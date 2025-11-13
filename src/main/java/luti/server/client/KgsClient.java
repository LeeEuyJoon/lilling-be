package luti.server.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import luti.server.client.dto.KeyBlock;
import luti.server.client.http.ExternalRestClient;
import luti.server.exception.BusinessException;

import static luti.server.exception.ErrorCode.*;

@Component
public class KgsClient {

    private final ExternalRestClient restClient;
    private final String kgsUrl;

    public KgsClient(ExternalRestClient restClient, @Value("${KGS_URL}") String kgsUrl) {
        this.restClient = restClient;
        this.kgsUrl = kgsUrl;
    }

    public KeyBlock fetchNextBlock() {
        String url = kgsUrl + "/api/v1/key/next-block";
        KeyBlock keyBlock = restClient.get(url, KeyBlock.class);

        if (keyBlock == null)
            throw new BusinessException(KGS_NULL_RESPONSE);
        if (keyBlock.getStart() > keyBlock.getEnd())
            throw new BusinessException(KGS_INVALID_RESPONSE);

        return keyBlock;
    }
}
