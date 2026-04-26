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
     * 方法说明：优先使用腾讯地图服务计算导航距离，失败时回退为本地直线距离。
     * 主要作用：这个方法把外部地图能力和本地兜底策略统一封装起来，供下单服务范围校验和首页推荐排序共同复用。
     * 实现逻辑：实现时会先判断腾讯地图配置是否启用，若启用则尝试请求导航距离；调用失败或返回异常时，再回退到 Haversine 公式计算直线距离。
     */
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
