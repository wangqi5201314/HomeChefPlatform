package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ChefServiceModeEnum;
import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.enums.TimeSlotEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.ChefRecommendQueryDTO;
import com.homechef.homechefsystem.entity.Chef;
import com.homechef.homechefsystem.entity.ChefSchedule;
import com.homechef.homechefsystem.entity.ChefServiceLocation;
import com.homechef.homechefsystem.entity.UserAddress;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.mapper.ChefScheduleMapper;
import com.homechef.homechefsystem.mapper.ChefServiceLocationMapper;
import com.homechef.homechefsystem.mapper.UserAddressMapper;
import com.homechef.homechefsystem.service.ChefRecommendService;
import com.homechef.homechefsystem.service.GeoDistanceService;
import com.homechef.homechefsystem.vo.ChefRecommendVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChefRecommendServiceImpl implements ChefRecommendService {

    private static final String SORT_DISTANCE = "DISTANCE";
    private static final String SORT_RATING = "RATING";
    private static final String SORT_ORDER_COUNT = "ORDER_COUNT";
    private static final String SORT_GOOD_REVIEW_RATE = "GOOD_REVIEW_RATE";
    private static final String SORT_DEFAULT = "DEFAULT";

    private final UserAddressMapper userAddressMapper;
    private final ChefMapper chefMapper;
    private final ChefServiceLocationMapper chefServiceLocationMapper;
    private final ChefScheduleMapper chefScheduleMapper;
    private final GeoDistanceService geoDistanceService;

    @Override
    public List<ChefRecommendVO> recommend(ChefRecommendQueryDTO chefRecommendQueryDTO) {
        validateIngredientMode(chefRecommendQueryDTO.getIngredientMode());
        String timeSlot = normalizeTimeSlot(chefRecommendQueryDTO.getTimeSlot());
        // 推荐接口以用户当前选择的地址为基准，后续所有服务范围判断都围绕该地址进行。
        UserAddress userAddress = requireUserAddress(
                chefRecommendQueryDTO.getUserId(),
                chefRecommendQueryDTO.getAddressId()
        );

        // 先批量拿候选厨师、启用服务位置和可用档期，再在内存中做组合过滤，避免前端多次循环请求。
        List<Chef> chefList = requireRecommendCandidates();
        List<Long> chefIds = extractChefIds(chefList);
        Map<Long, ChefServiceLocation> activeLocationMap = buildActiveLocationMap(chefIds);
        Set<Long> availableChefIdSet = buildAvailableChefIdSet(chefRecommendQueryDTO.getServiceDate(), timeSlot);

        List<ChefRecommendVO> recommendVOList = chefList.stream()
                .filter(chef -> supportsIngredientMode(chef.getServiceMode(), chefRecommendQueryDTO.getIngredientMode()))
                .filter(chef -> availableChefIdSet.contains(chef.getId()))
                .map(chef -> toChefRecommendVO(chef, activeLocationMap.get(chef.getId()), userAddress))
                .filter(java.util.Objects::nonNull)
                .sorted(buildComparator(normalizeSortType(chefRecommendQueryDTO.getSortType())))
                .collect(Collectors.toList());

        if (recommendVOList.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "无可推荐厨师");
        }
        return recommendVOList;
    }

    @Override
    public List<ChefRecommendVO> recommendDefault(Long userId, Long addressId) {
        UserAddress userAddress = requireUserAddress(userId, addressId);
        List<Chef> chefList = requireRecommendCandidates();
        List<Long> chefIds = extractChefIds(chefList);
        Map<Long, ChefServiceLocation> activeLocationMap = buildActiveLocationMap(chefIds);
        // 首页默认推荐不限定时段，只要未来7天内存在任意可预约档期即可进入候选。
        Map<Long, ChefSchedule> nearestScheduleMap = buildNearestScheduleMap(chefIds);

        List<ChefRecommendVO> recommendVOList = chefList.stream()
                .map(chef -> toDefaultChefRecommendVO(
                        chef,
                        activeLocationMap.get(chef.getId()),
                        nearestScheduleMap.get(chef.getId()),
                        userAddress
                ))
                .filter(java.util.Objects::nonNull)
                .sorted(buildDefaultHomeComparator())
                .collect(Collectors.toList());

        if (recommendVOList.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "无可推荐厨师");
        }
        return recommendVOList;
    }

    private List<Chef> requireRecommendCandidates() {
        List<Chef> chefList = chefMapper.selectRecommendCandidates();
        if (chefList == null || chefList.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "无可推荐厨师");
        }
        return chefList;
    }

    private List<Long> extractChefIds(List<Chef> chefList) {
        if (chefList == null || chefList.isEmpty()) {
            return Collections.emptyList();
        }
        return chefList.stream()
                .map(Chef::getId)
                .collect(Collectors.toList());
    }

    private Map<Long, ChefServiceLocation> buildActiveLocationMap(List<Long> chefIds) {
        if (chefIds == null || chefIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 厨师可能有多个服务位置，但推荐和下单只使用当前启用中的那一个。
        List<ChefServiceLocation> activeLocationList = chefServiceLocationMapper.selectActiveListByChefIds(chefIds);
        if (activeLocationList == null || activeLocationList.isEmpty()) {
            return Collections.emptyMap();
        }
        return activeLocationList.stream().collect(Collectors.toMap(
                ChefServiceLocation::getChefId,
                chefServiceLocation -> chefServiceLocation,
                (left, right) -> left,
                LinkedHashMap::new
        ));
    }

    private Map<Long, ChefSchedule> buildNearestScheduleMap(List<Long> chefIds) {
        if (chefIds == null || chefIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(6);
        List<ChefSchedule> chefScheduleList = chefScheduleMapper.selectAvailableListByChefIdsAndDateRange(
                chefIds,
                startDate,
                endDate
        );
        if (chefScheduleList == null || chefScheduleList.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, ChefSchedule> nearestScheduleMap = new LinkedHashMap<>();
        for (ChefSchedule chefSchedule : chefScheduleList) {
            if (chefSchedule == null || chefSchedule.getChefId() == null) {
                continue;
            }
            // SQL 已按日期和开始时间升序排列，第一次放入的就是该厨师最近可预约档期。
            nearestScheduleMap.putIfAbsent(chefSchedule.getChefId(), chefSchedule);
        }
        return nearestScheduleMap;
    }

    private Set<Long> buildAvailableChefIdSet(LocalDate serviceDate, String timeSlot) {
        List<Long> availableChefIds = chefScheduleMapper.selectAvailableChefIdsByDateAndTimeSlot(serviceDate, timeSlot);
        if (availableChefIds == null || availableChefIds.isEmpty()) {
            return Collections.emptySet();
        }
        return Set.copyOf(availableChefIds);
    }

    private UserAddress requireUserAddress(Long userId, Long addressId) {
        UserAddress userAddress = userAddressMapper.selectByIdAndUserId(addressId, userId);
        if (userAddress == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "地址不存在");
        }
        if (userAddress.getLongitude() == null || userAddress.getLatitude() == null) {
            throw new BusinessException(ResultCodeEnum.FAIL, "地址坐标缺失");
        }
        return userAddress;
    }

    private ChefRecommendVO toChefRecommendVO(Chef chef, ChefServiceLocation chefServiceLocation, UserAddress userAddress) {
        if (chef == null || chefServiceLocation == null) {
            return null;
        }
        // 返回 null 表示该厨师不满足推荐条件，调用方会统一 filter 掉。
        if (chef.getServiceRadiusKm() == null || chef.getServiceRadiusKm() <= 0) {
            return null;
        }
        if (chefServiceLocation.getLongitude() == null || chefServiceLocation.getLatitude() == null) {
            return null;
        }

        double distanceKm = geoDistanceService.distanceKm(
                userAddress.getLatitude(),
                userAddress.getLongitude(),
                chefServiceLocation.getLatitude(),
                chefServiceLocation.getLongitude()
        );
        if (distanceKm > chef.getServiceRadiusKm()) {
            return null;
        }

        return ChefRecommendVO.builder()
                .id(chef.getId())
                .name(chef.getName())
                .avatar(chef.getAvatar())
                .specialtyCuisine(chef.getSpecialtyCuisine())
                .yearsOfExperience(chef.getYearsOfExperience())
                .ratingAvg(defaultBigDecimal(chef.getRatingAvg()))
                .orderCount(defaultInteger(chef.getOrderCount()))
                .goodReviewRate(defaultBigDecimal(chef.getGoodReviewRate()))
                .serviceMode(chef.getServiceMode())
                .serviceModeDesc(ChefServiceModeEnum.getDescByCode(chef.getServiceMode()))
                .serviceRadiusKm(chef.getServiceRadiusKm())
                .serviceAreaText(buildServiceAreaText(chefServiceLocation))
                .distanceKm(BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private ChefRecommendVO toDefaultChefRecommendVO(
            Chef chef,
            ChefServiceLocation chefServiceLocation,
            ChefSchedule nearestSchedule,
            UserAddress userAddress
    ) {
        ChefRecommendVO chefRecommendVO = toChefRecommendVO(chef, chefServiceLocation, userAddress);
        if (chefRecommendVO == null || nearestSchedule == null) {
            return null;
        }
        chefRecommendVO.setNearestAvailableDate(nearestSchedule.getServiceDate());
        chefRecommendVO.setNearestAvailableTimeSlot(nearestSchedule.getTimeSlot());
        chefRecommendVO.setNearestAvailableTimeSlotDesc(TimeSlotEnum.getDescByCode(nearestSchedule.getTimeSlot()));
        return chefRecommendVO;
    }

    private Comparator<ChefRecommendVO> buildComparator(String sortType) {
        Comparator<ChefRecommendVO> defaultComparator = Comparator
                .comparing(ChefRecommendVO::getDistanceKm, this::compareBigDecimalAsc)
                .thenComparing(ChefRecommendVO::getRatingAvg, this::compareBigDecimalDesc)
                .thenComparing(ChefRecommendVO::getOrderCount, this::compareIntegerDesc)
                .thenComparing(ChefRecommendVO::getGoodReviewRate, this::compareBigDecimalDesc);

        return switch (sortType) {
            case SORT_DISTANCE -> defaultComparator;
            case SORT_RATING -> Comparator
                    .comparing(ChefRecommendVO::getRatingAvg, this::compareBigDecimalDesc)
                    .thenComparing(ChefRecommendVO::getDistanceKm, this::compareBigDecimalAsc)
                    .thenComparing(ChefRecommendVO::getOrderCount, this::compareIntegerDesc)
                    .thenComparing(ChefRecommendVO::getGoodReviewRate, this::compareBigDecimalDesc);
            case SORT_ORDER_COUNT -> Comparator
                    .comparing(ChefRecommendVO::getOrderCount, this::compareIntegerDesc)
                    .thenComparing(ChefRecommendVO::getDistanceKm, this::compareBigDecimalAsc)
                    .thenComparing(ChefRecommendVO::getRatingAvg, this::compareBigDecimalDesc)
                    .thenComparing(ChefRecommendVO::getGoodReviewRate, this::compareBigDecimalDesc);
            case SORT_GOOD_REVIEW_RATE -> Comparator
                    .comparing(ChefRecommendVO::getGoodReviewRate, this::compareBigDecimalDesc)
                    .thenComparing(ChefRecommendVO::getDistanceKm, this::compareBigDecimalAsc)
                    .thenComparing(ChefRecommendVO::getRatingAvg, this::compareBigDecimalDesc)
                    .thenComparing(ChefRecommendVO::getOrderCount, this::compareIntegerDesc);
            default -> defaultComparator;
        };
    }

    private Comparator<ChefRecommendVO> buildDefaultHomeComparator() {
        // 首页默认排序优先让用户看到“最近可约”的厨师，再兼顾距离、评分、订单量和好评率。
        return Comparator
                .comparing(ChefRecommendVO::getNearestAvailableDate, Comparator.nullsLast(LocalDate::compareTo))
                .thenComparing(ChefRecommendVO::getDistanceKm, this::compareBigDecimalAsc)
                .thenComparing(ChefRecommendVO::getRatingAvg, this::compareBigDecimalDesc)
                .thenComparing(ChefRecommendVO::getOrderCount, this::compareIntegerDesc)
                .thenComparing(ChefRecommendVO::getGoodReviewRate, this::compareBigDecimalDesc);
    }

    private int compareBigDecimalAsc(BigDecimal left, BigDecimal right) {
        return defaultBigDecimal(left).compareTo(defaultBigDecimal(right));
    }

    private int compareBigDecimalDesc(BigDecimal left, BigDecimal right) {
        return defaultBigDecimal(right).compareTo(defaultBigDecimal(left));
    }

    private int compareIntegerDesc(Integer left, Integer right) {
        return Integer.compare(defaultInteger(right), defaultInteger(left));
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean supportsIngredientMode(Integer serviceMode, Integer ingredientMode) {
        if (serviceMode == null || ingredientMode == null) {
            return false;
        }
        if (ingredientMode == 1) {
            return serviceMode == 1 || serviceMode == 3;
        }
        if (ingredientMode == 2) {
            return serviceMode == 2 || serviceMode == 3;
        }
        return false;
    }

    private void validateIngredientMode(Integer ingredientMode) {
        if (ingredientMode == null || (ingredientMode != 1 && ingredientMode != 2)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "ingredientMode 取值非法，只能为 1 或 2");
        }
    }

    private String normalizeTimeSlot(String timeSlot) {
        TimeSlotEnum timeSlotEnum = TimeSlotEnum.fromCode(timeSlot);
        if (timeSlotEnum == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, TimeSlotEnum.INVALID_MESSAGE);
        }
        return timeSlotEnum.getCode();
    }

    private String normalizeSortType(String sortType) {
        if (!StringUtils.hasText(sortType)) {
            return SORT_DEFAULT;
        }
        return sortType.trim().toUpperCase(Locale.ROOT);
    }

    private String buildServiceAreaText(ChefServiceLocation chefServiceLocation) {
        if (chefServiceLocation == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        appendAreaPart(builder, chefServiceLocation.getProvince());
        appendAreaPart(builder, chefServiceLocation.getCity());
        appendAreaPart(builder, chefServiceLocation.getDistrict());
        appendAreaPart(builder, chefServiceLocation.getTown());
        return builder.length() == 0 ? null : builder.toString();
    }

    private void appendAreaPart(StringBuilder builder, String areaPart) {
        if (StringUtils.hasText(areaPart)) {
            builder.append(areaPart.trim());
        }
    }
}
