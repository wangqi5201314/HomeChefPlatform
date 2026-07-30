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

    /**
     * 计算两个坐标之间的距离，单位是公里。
     * 这个方法主要给推荐和下单校验使用，用来判断用户地址是否在厨师的服务范围内。
     * 它会优先尝试调用腾讯地图计算导航距离，如果调用失败，就自动退回到本地 Haversine 直线距离计算。
     */
    @Override
    public double distanceKm(BigDecimal fromLatitude,
                             BigDecimal fromLongitude,
                             BigDecimal toLatitude,
                             BigDecimal toLongitude) {
        if (fromLatitude == null || fromLongitude == null || toLatitude == null || toLongitude == null) {
            throw new IllegalArgumentException("coordinates must not be null");
        }

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
