package com.homechef.homechefsystem.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homechef.homechefsystem.config.TencentMapProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TencentMapClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TencentMapProperties tencentMapProperties;

    public Optional<Double> getDrivingDistanceKm(BigDecimal fromLatitude,
                                                 BigDecimal fromLongitude,
                                                 BigDecimal toLatitude,
                                                 BigDecimal toLongitude) {
        // 未开启或未配置 key 时直接返回 empty，由上层统一回退为本地距离计算。
        if (!isAvailable()) {
            return Optional.empty();
        }

        String url = buildDistanceMatrixUrl(fromLatitude, fromLongitude, toLatitude, toLongitude);
        RestTemplate restTemplate = buildRestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String body = response.getBody();
            if (!StringUtils.hasText(body)) {
                return Optional.empty();
            }
            JsonNode root = OBJECT_MAPPER.readTree(body);
            if (root.path("status").asInt(-1) != 0) {
                log.warn("tencent map distance request failed: status={}, message={}",
                        root.path("status").asText(),
                        root.path("message").asText());
                return Optional.empty();
            }

            // 腾讯地图距离矩阵返回的 distance 单位是米，项目统一对外使用公里。
            JsonNode distanceNode = root.path("result").path("rows").path(0).path("elements").path(0).path("distance");
            if (distanceNode.isMissingNode() || distanceNode.isNull()) {
                return Optional.empty();
            }

            return Optional.of(distanceNode.asDouble() / 1000D);
        } catch (RestClientException e) {
            log.warn("tencent map request failed: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("failed to parse tencent map response: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private boolean isAvailable() {
        return tencentMapProperties.isEnabled() && StringUtils.hasText(tencentMapProperties.getKey());
    }

    private String buildDistanceMatrixUrl(BigDecimal fromLatitude,
                                          BigDecimal fromLongitude,
                                          BigDecimal toLatitude,
                                          BigDecimal toLongitude) {
        String baseUrl = tencentMapProperties.getBaseUrl();
        String distanceMatrixPath = tencentMapProperties.getDistanceMatrixPath();
        String from = fromLatitude + "," + fromLongitude;
        String to = toLatitude + "," + toLongitude;

        return UriComponentsBuilder.fromHttpUrl(baseUrl + distanceMatrixPath)
                .queryParam("mode", tencentMapProperties.getMode())
                .queryParam("from", from)
                .queryParam("to", to)
                .queryParam("key", tencentMapProperties.getKey())
                .build()
                .toUriString();
    }

    private RestTemplate buildRestTemplate() {
        // 每次构造带超时的 RestTemplate，避免地图接口慢响应拖住下单和首页推荐接口。
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(tencentMapProperties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(tencentMapProperties.getReadTimeoutMs());
        return new RestTemplate(requestFactory);
    }
}
