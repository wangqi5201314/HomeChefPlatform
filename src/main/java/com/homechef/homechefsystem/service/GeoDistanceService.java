package com.homechef.homechefsystem.service;

import java.math.BigDecimal;

public interface GeoDistanceService {

    double distanceKm(BigDecimal fromLatitude,
                      BigDecimal fromLongitude,
                      BigDecimal toLatitude,
                      BigDecimal toLongitude);
}
