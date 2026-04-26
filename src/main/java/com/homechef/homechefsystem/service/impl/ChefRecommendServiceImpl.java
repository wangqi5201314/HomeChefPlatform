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
     * 方法说明：按用户选择的地址、食材模式、日期、时段和排序规则返回可推荐厨师列表。
     * 主要作用：这是首页定向推荐的核心方法，用来一次性完成候选过滤、距离判断和排序，避免前端循环请求多个接口自行拼装。
     * 实现逻辑：方法会先校验入参并获取用户地址，然后批量加载候选厨师、启用服务位置和可预约档期，再在内存中过滤服务范围、食材模式和档期条件，最后按指定规则排序后返回。
     */
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

    /**
     * 方法说明：返回首页默认推荐的厨师列表，条件为未来七天内至少存在一个可预约档期。
     * 主要作用：它用于用户未选择具体日期和时段时的首页展示，让系统优先给出近期可约且距离合适的厨师。
     * 实现逻辑：方法会先校验默认地址，再批量加载候选厨师、启用位置和未来七天档期，筛出可服务且有最近可预约时间的厨师，并按默认综合规则排序。
     */
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

    /**
     * 方法说明：获取当前业务必需的数据，并在取不到时立即中断流程。
     * 主要作用：它把 首页厨师推荐服务实现 中“查询 + 非空校验”的重复套路合并成一个辅助方法，让主流程更聚焦业务本身。
     * 实现逻辑：实现时会先根据身份信息或业务键查询目标数据，再补充坐标、状态或归属校验，不满足条件时直接抛出业务异常。
     */
    private List<Chef> requireRecommendCandidates() {
        List<Chef> chefList = chefMapper.selectRecommendCandidates();
        if (chefList == null || chefList.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "无可推荐厨师");
        }
        return chefList;
    }

    /**
     * 方法说明：在 首页厨师推荐服务实现 中处理 extractChefIds 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
     * 方法说明：构建当前业务流程后续需要复用的中间结果。
     * 主要作用：这个辅助方法把 首页厨师推荐服务实现 中重复使用的数据结构提前整理好，减少主流程中的重复计算和分支判断。
     * 实现逻辑：实现逻辑通常会对输入参数做空值保护，再根据业务规则拼装映射、集合、文本或比较器，供后续步骤直接复用。
     */
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

    /**
     * 方法说明：构建当前业务流程后续需要复用的中间结果。
     * 主要作用：这个辅助方法把 首页厨师推荐服务实现 中重复使用的数据结构提前整理好，减少主流程中的重复计算和分支判断。
     * 实现逻辑：实现逻辑通常会对输入参数做空值保护，再根据业务规则拼装映射、集合、文本或比较器，供后续步骤直接复用。
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
            // SQL 已按日期和开始时间升序排列，第一次放入的就是该厨师最近可预约档期。
            nearestScheduleMap.putIfAbsent(chefSchedule.getChefId(), chefSchedule);
        }
        return nearestScheduleMap;
    }

    /**
     * 方法说明：构建当前业务流程后续需要复用的中间结果。
     * 主要作用：这个辅助方法把 首页厨师推荐服务实现 中重复使用的数据结构提前整理好，减少主流程中的重复计算和分支判断。
     * 实现逻辑：实现逻辑通常会对输入参数做空值保护，再根据业务规则拼装映射、集合、文本或比较器，供后续步骤直接复用。
     */
    private Set<Long> buildAvailableChefIdSet(LocalDate serviceDate, String timeSlot) {
        List<Long> availableChefIds = chefScheduleMapper.selectAvailableChefIdsByDateAndTimeSlot(serviceDate, timeSlot);
        if (availableChefIds == null || availableChefIds.isEmpty()) {
            return Collections.emptySet();
        }
        return Set.copyOf(availableChefIds);
    }

    /**
     * 方法说明：获取当前业务必需的数据，并在取不到时立即中断流程。
     * 主要作用：它把 首页厨师推荐服务实现 中“查询 + 非空校验”的重复套路合并成一个辅助方法，让主流程更聚焦业务本身。
     * 实现逻辑：实现时会先根据身份信息或业务键查询目标数据，再补充坐标、状态或归属校验，不满足条件时直接抛出业务异常。
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
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 首页厨师推荐服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
     */
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

    /**
     * 方法说明：将实体对象或中间结果转换为接口返回所需的 VO 对象。
     * 主要作用：该方法把 首页厨师推荐服务实现 中对外展示需要的字段映射集中在一起，避免多个业务入口重复编写相同的转换代码。
     * 实现逻辑：实现时会先判断入参是否为空，然后逐项拷贝基础字段，必要时补充枚举描述、派生文本或关联展示信息后返回。
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
     * 方法说明：构建当前业务流程后续需要复用的中间结果。
     * 主要作用：这个辅助方法把 首页厨师推荐服务实现 中重复使用的数据结构提前整理好，减少主流程中的重复计算和分支判断。
     * 实现逻辑：实现逻辑通常会对输入参数做空值保护，再根据业务规则拼装映射、集合、文本或比较器，供后续步骤直接复用。
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
     * 方法说明：构建当前业务流程后续需要复用的中间结果。
     * 主要作用：这个辅助方法把 首页厨师推荐服务实现 中重复使用的数据结构提前整理好，减少主流程中的重复计算和分支判断。
     * 实现逻辑：实现逻辑通常会对输入参数做空值保护，再根据业务规则拼装映射、集合、文本或比较器，供后续步骤直接复用。
     */
    private Comparator<ChefRecommendVO> buildDefaultHomeComparator() {
        // 首页默认排序优先让用户看到“最近可约”的厨师，再兼顾距离、评分、订单量和好评率。
        return Comparator
                .comparing(ChefRecommendVO::getNearestAvailableDate, Comparator.nullsLast(LocalDate::compareTo))
                .thenComparing(ChefRecommendVO::getDistanceKm, this::compareBigDecimalAsc)
                .thenComparing(ChefRecommendVO::getRatingAvg, this::compareBigDecimalDesc)
                .thenComparing(ChefRecommendVO::getOrderCount, this::compareIntegerDesc)
                .thenComparing(ChefRecommendVO::getGoodReviewRate, this::compareBigDecimalDesc);
    }

    /**
     * 方法说明：比较两个排序字段的大小关系。
     * 主要作用：它用于封装 首页厨师推荐服务实现 中的局部排序规则，减少主比较器里重复书写空值处理和升降序细节。
     * 实现逻辑：实现时会先把可能为空的字段转成安全值，再按约定的升序或降序规则返回比较结果。
     */
    private int compareBigDecimalAsc(BigDecimal left, BigDecimal right) {
        return defaultBigDecimal(left).compareTo(defaultBigDecimal(right));
    }

    /**
     * 方法说明：比较两个排序字段的大小关系。
     * 主要作用：它用于封装 首页厨师推荐服务实现 中的局部排序规则，减少主比较器里重复书写空值处理和升降序细节。
     * 实现逻辑：实现时会先把可能为空的字段转成安全值，再按约定的升序或降序规则返回比较结果。
     */
    private int compareBigDecimalDesc(BigDecimal left, BigDecimal right) {
        return defaultBigDecimal(right).compareTo(defaultBigDecimal(left));
    }

    /**
     * 方法说明：比较两个排序字段的大小关系。
     * 主要作用：它用于封装 首页厨师推荐服务实现 中的局部排序规则，减少主比较器里重复书写空值处理和升降序细节。
     * 实现逻辑：实现时会先把可能为空的字段转成安全值，再按约定的升序或降序规则返回比较结果。
     */
    private int compareIntegerDesc(Integer left, Integer right) {
        return Integer.compare(defaultInteger(right), defaultInteger(left));
    }

    /**
     * 方法说明：为可能为空的排序或计算字段提供默认值。
     * 主要作用：它用于保证 首页厨师推荐服务实现 中的数值比较逻辑稳定执行，避免空指针影响排序或统计结果。
     * 实现逻辑：实现时会先判断传入值是否为空；若为空则返回预设默认值，否则直接返回原值。
     */
    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 方法说明：为可能为空的排序或计算字段提供默认值。
     * 主要作用：它用于保证 首页厨师推荐服务实现 中的数值比较逻辑稳定执行，避免空指针影响排序或统计结果。
     * 实现逻辑：实现时会先判断传入值是否为空；若为空则返回预设默认值，否则直接返回原值。
     */
    private Integer defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 方法说明：在 首页厨师推荐服务实现 中处理 supportsIngredientMode 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
     * 方法说明：校验当前业务输入或状态是否满足执行条件。
     * 主要作用：它用于把 首页厨师推荐服务实现 中的前置规则集中收口，避免核心流程夹杂过多重复的条件判断。
     * 实现逻辑：实现逻辑会逐项检查关键字段、状态或业务约束，一旦发现不满足条件的情况就立即抛出业务异常阻断流程。
     */
    private void validateIngredientMode(Integer ingredientMode) {
        if (ingredientMode == null || (ingredientMode != 1 && ingredientMode != 2)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "ingredientMode 取值非法，只能为 1 或 2");
        }
    }

    /**
     * 方法说明：对输入值做统一的格式化和规范化处理。
     * 主要作用：该方法用于消除 首页厨师推荐服务实现 中大小写、空白字符或别名写法带来的差异，保证后续逻辑按统一格式处理数据。
     * 实现逻辑：实现时会先做空值判断，再进行 trim、大小写转换或枚举标准化，最终返回可直接参与业务判断的值。
     */
    private String normalizeTimeSlot(String timeSlot) {
        TimeSlotEnum timeSlotEnum = TimeSlotEnum.fromCode(timeSlot);
        if (timeSlotEnum == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, TimeSlotEnum.INVALID_MESSAGE);
        }
        return timeSlotEnum.getCode();
    }

    /**
     * 方法说明：对输入值做统一的格式化和规范化处理。
     * 主要作用：该方法用于消除 首页厨师推荐服务实现 中大小写、空白字符或别名写法带来的差异，保证后续逻辑按统一格式处理数据。
     * 实现逻辑：实现时会先做空值判断，再进行 trim、大小写转换或枚举标准化，最终返回可直接参与业务判断的值。
     */
    private String normalizeSortType(String sortType) {
        if (!StringUtils.hasText(sortType)) {
            return SORT_DEFAULT;
        }
        return sortType.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 方法说明：构建当前业务流程后续需要复用的中间结果。
     * 主要作用：这个辅助方法把 首页厨师推荐服务实现 中重复使用的数据结构提前整理好，减少主流程中的重复计算和分支判断。
     * 实现逻辑：实现逻辑通常会对输入参数做空值保护，再根据业务规则拼装映射、集合、文本或比较器，供后续步骤直接复用。
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
     * 方法说明：在 首页厨师推荐服务实现 中处理 appendAreaPart 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
     */
    private void appendAreaPart(StringBuilder builder, String areaPart) {
        if (StringUtils.hasText(areaPart)) {
            builder.append(areaPart.trim());
        }
    }
}
