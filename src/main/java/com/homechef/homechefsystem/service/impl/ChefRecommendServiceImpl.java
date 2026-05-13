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

    /**
     * 按照前端传入的条件返回可推荐的厨师列表。
     * 这个方法主要用在首页推荐中，把可接单、可服务、有档期的厨师一次性筛出来。
     * 它会先获取用户地址，再批量查厨师、档期和服务位置，最后按距离、评分等规则进行排序。
     */
    @Override
    public List<ChefRecommendVO> recommend(ChefRecommendQueryDTO chefRecommendQueryDTO) {
        validateIngredientMode(chefRecommendQueryDTO.getIngredientMode());
        String timeSlot = normalizeTimeSlot(chefRecommendQueryDTO.getTimeSlot());
        UserAddress userAddress = requireUserAddress(
                chefRecommendQueryDTO.getUserId(),
                chefRecommendQueryDTO.getAddressId()
        );

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

    /**
     * 返回首页默认展示的厨师推荐列表。
     * 这个方法给用户未选日期和时段时使用，只展示近七天内有可约档期的厨师。
     * 它会先检查用户地址，再批量找出候选厨师和最近的可约档期，然后按默认规则排序返回。
     */
    @Override
    public List<ChefRecommendVO> recommendDefault(Long userId, Long addressId) {
        UserAddress userAddress = requireUserAddress(userId, addressId);
        List<Chef> chefList = requireRecommendCandidates();
        List<Long> chefIds = extractChefIds(chefList);
        Map<Long, ChefServiceLocation> activeLocationMap = buildActiveLocationMap(chefIds);
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

    /**
     * 查出当前业务必须要用的数据。
     * 这个方法用于把“先查数据，找不到就报错”这类逻辑集中到一起。
     * 它会根据 id 或当前登录信息去查记录，如果查不到或不符合条件，就直接抛出异常。
     */
    private List<Chef> requireRecommendCandidates() {
        List<Chef> chefList = chefMapper.selectRecommendCandidates();
        if (chefList == null || chefList.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "无可推荐厨师");
        }
        return chefList;
    }

    /**
     * 处理 extractChefIds 这个方法对应的业务逻辑。
     * 这个方法主要是把当前模块里的某一段独立工作单独拆出来，让主流程更清楚。
     * 它会围绕自己的职责去查询数据、处理规则，最后返回结果或更新状态。
     */
    private List<Long> extractChefIds(List<Chef> chefList) {
        if (chefList == null || chefList.isEmpty()) {
            return Collections.emptyList();
        }
        return chefList.stream()
                .map(Chef::getId)
                .collect(Collectors.toList());
    }

    /**
     * 构建一个后续会被重复使用的中间结果。
     * 这个方法主要是为了把主流程里的细节拆出去，让主流程更容易看。
     * 它会根据当前需要把集合、映射、路径、文本或比较器等内容先准备好。
     */
    private Map<Long, ChefServiceLocation> buildActiveLocationMap(List<Long> chefIds) {
        if (chefIds == null || chefIds.isEmpty()) {
            return Collections.emptyMap();
        }
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

    /**
     * 构建一个后续会被重复使用的中间结果。
     * 这个方法主要是为了把主流程里的细节拆出去，让主流程更容易看。
     * 它会根据当前需要把集合、映射、路径、文本或比较器等内容先准备好。
     */
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
            nearestScheduleMap.putIfAbsent(chefSchedule.getChefId(), chefSchedule);
        }
        return nearestScheduleMap;
    }

    /**
     * 构建一个后续会被重复使用的中间结果。
     * 这个方法主要是为了把主流程里的细节拆出去，让主流程更容易看。
     * 它会根据当前需要把集合、映射、路径、文本或比较器等内容先准备好。
     */
    private Set<Long> buildAvailableChefIdSet(LocalDate serviceDate, String timeSlot) {
        List<Long> availableChefIds = chefScheduleMapper.selectAvailableChefIdsByDateAndTimeSlot(serviceDate, timeSlot);
        if (availableChefIds == null || availableChefIds.isEmpty()) {
            return Collections.emptySet();
        }
        return Set.copyOf(availableChefIds);
    }

    /**
     * 查出当前业务必须要用的数据。
     * 这个方法用于把“先查数据，找不到就报错”这类逻辑集中到一起。
     * 它会根据 id 或当前登录信息去查记录，如果查不到或不符合条件，就直接抛出异常。
     */
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

    /**
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
     */
    private ChefRecommendVO toChefRecommendVO(Chef chef, ChefServiceLocation chefServiceLocation, UserAddress userAddress) {
        if (chef == null || chefServiceLocation == null) {
            return null;
        }
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

    /**
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
     */
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

    /**
     * 构建一个后续会被重复使用的中间结果。
     * 这个方法主要是为了把主流程里的细节拆出去，让主流程更容易看。
     * 它会根据当前需要把集合、映射、路径、文本或比较器等内容先准备好。
     */
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

    /**
     * 构建一个后续会被重复使用的中间结果。
     * 这个方法主要是为了把主流程里的细节拆出去，让主流程更容易看。
     * 它会根据当前需要把集合、映射、路径、文本或比较器等内容先准备好。
     */
    private Comparator<ChefRecommendVO> buildDefaultHomeComparator() {
        return Comparator
                .comparing(ChefRecommendVO::getNearestAvailableDate, Comparator.nullsLast(LocalDate::compareTo))
                .thenComparing(ChefRecommendVO::getDistanceKm, this::compareBigDecimalAsc)
                .thenComparing(ChefRecommendVO::getRatingAvg, this::compareBigDecimalDesc)
                .thenComparing(ChefRecommendVO::getOrderCount, this::compareIntegerDesc)
                .thenComparing(ChefRecommendVO::getGoodReviewRate, this::compareBigDecimalDesc);
    }

    /**
     * 比较两个值的排序顺序。
     * 这个方法让排序规则写得更清楚，也方便在多个地方重复使用。
     * 它会先处理空值，再按升序或降序返回比较结果。
     */
    private int compareBigDecimalAsc(BigDecimal left, BigDecimal right) {
        return defaultBigDecimal(left).compareTo(defaultBigDecimal(right));
    }

    /**
     * 比较两个值的排序顺序。
     * 这个方法让排序规则写得更清楚，也方便在多个地方重复使用。
     * 它会先处理空值，再按升序或降序返回比较结果。
     */
    private int compareBigDecimalDesc(BigDecimal left, BigDecimal right) {
        return defaultBigDecimal(right).compareTo(defaultBigDecimal(left));
    }

    /**
     * 比较两个值的排序顺序。
     * 这个方法让排序规则写得更清楚，也方便在多个地方重复使用。
     * 它会先处理空值，再按升序或降序返回比较结果。
     */
    private int compareIntegerDesc(Integer left, Integer right) {
        return Integer.compare(defaultInteger(right), defaultInteger(left));
    }

    /**
     * 给可能为空的值补一个默认值。
     * 这个方法主要是为了让排序和计算更稳定，避免出现空指针。
     * 它会先判断值是不是空，如果是空就返回默认值，不是空就原样返回。
     */
    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 给可能为空的值补一个默认值。
     * 这个方法主要是为了让排序和计算更稳定，避免出现空指针。
     * 它会先判断值是不是空，如果是空就返回默认值，不是空就原样返回。
     */
    private Integer defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 判断当前配置是不是支持某个条件。
     * 这个方法主要用来做模式匹配或能力判断，让主流程更直接。
     * 它会把当前值和目标条件做比对，最后返回 true 或 false。
     */
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

    /**
     * 检查当前传入的参数或业务状态是否合法。
     * 这个方法的作用，是把不合条件的情况尽早拦住，不让错误数据继续往下执行。
     * 它会根据规则逐项检查参数或状态，只要发现不满足条件，就直接抛出异常。
     */
    private void validateIngredientMode(Integer ingredientMode) {
        if (ingredientMode == null || (ingredientMode != 1 && ingredientMode != 2)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "ingredientMode 取值非法，只能为 1 或 2");
        }
    }

    /**
     * 把输入值整理成统一的格式。
     * 这个方法可以避免因为大小写、空格或不同写法导致后面的业务判断出错。
     * 它通常会先做 trim，再统一大小写，或者转成系统里约定好的标准值。
     */
    private String normalizeTimeSlot(String timeSlot) {
        TimeSlotEnum timeSlotEnum = TimeSlotEnum.fromCode(timeSlot);
        if (timeSlotEnum == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, TimeSlotEnum.INVALID_MESSAGE);
        }
        return timeSlotEnum.getCode();
    }

    /**
     * 把输入值整理成统一的格式。
     * 这个方法可以避免因为大小写、空格或不同写法导致后面的业务判断出错。
     * 它通常会先做 trim，再统一大小写，或者转成系统里约定好的标准值。
     */
    private String normalizeSortType(String sortType) {
        if (!StringUtils.hasText(sortType)) {
            return SORT_DEFAULT;
        }
        return sortType.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 构建一个后续会被重复使用的中间结果。
     * 这个方法主要是为了把主流程里的细节拆出去，让主流程更容易看。
     * 它会根据当前需要把集合、映射、路径、文本或比较器等内容先准备好。
     */
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

    /**
     * 处理 appendAreaPart 这个方法对应的业务逻辑。
     * 这个方法主要是把当前模块里的某一段独立工作单独拆出来，让主流程更清楚。
     * 它会围绕自己的职责去查询数据、处理规则，最后返回结果或更新状态。
     */
    private void appendAreaPart(StringBuilder builder, String areaPart) {
        if (StringUtils.hasText(areaPart)) {
            builder.append(areaPart.trim());
        }
    }
}
