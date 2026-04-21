package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.client.TencentMapClient;
import com.homechef.homechefsystem.service.GeoDistanceService;
import com.homechef.homechefsystem.utils.GeoDistanceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoDistanceServiceImpl implements GeoDistanceService {

    private final TencentMapClient tencentMapClient;

    @Override
    public double distanceKm(BigDecimal fromLatitude,
                             BigDecimal fromLongitude,
                             BigDecimal toLatitude,
                             BigDecimal toLongitude) {
        if (fromLatitude == null || fromLongitude == null || toLatitude == null || toLongitude == null) {
            throw new IllegalArgumentException("coordinates must not be null");
        }

        // 业务上更希望使用道路导航距离；腾讯地图不可用时回退直线距离，避免下单/推荐被第三方服务阻断。
        return tencentMapClient.getDrivingDistanceKm(fromLatitude, fromLongitude, toLatitude, toLongitude)
                .orElseGet(() -> {
                    log.debug("fallback to haversine distance");
                    return GeoDistanceUtil.distanceKm(
                            fromLatitude.doubleValue(),
                            fromLongitude.doubleValue(),
                            toLatitude.doubleValue(),
                            toLongitude.doubleValue()
                    );
                });
    }
}
