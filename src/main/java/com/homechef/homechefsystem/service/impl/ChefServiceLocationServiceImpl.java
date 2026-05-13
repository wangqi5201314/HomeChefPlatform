package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.dto.ChefServiceLocationCreateDTO;
import com.homechef.homechefsystem.dto.ChefServiceLocationUpdateDTO;
import com.homechef.homechefsystem.entity.Chef;
import com.homechef.homechefsystem.entity.ChefServiceLocation;
import com.homechef.homechefsystem.mapper.ChefMapper;
import com.homechef.homechefsystem.mapper.ChefServiceLocationMapper;
import com.homechef.homechefsystem.service.ChefServiceLocationService;
import com.homechef.homechefsystem.utils.LoginUserContext;
import com.homechef.homechefsystem.vo.ChefServiceLocationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChefServiceLocationServiceImpl implements ChefServiceLocationService {

    private final ChefServiceLocationMapper chefServiceLocationMapper;
    private final ChefMapper chefMapper;

    /**
     * 查询一组符合条件的列表数据。
     * 这个方法主要给列表页面或管理页面使用，让调用方可以一次拿到需要的数据。
     * 它会根据传入的条件查数据，如果需要的话还会把结果转成接口要返回的对象。
     */
    @Override
    public List<ChefServiceLocationVO> getCurrentChefServiceLocationList() {
        Long chefId = requireCurrentChefId();
        requireChefExists(chefId);
        List<ChefServiceLocation> chefServiceLocationList = chefServiceLocationMapper.selectListByChefId(chefId);
        if (chefServiceLocationList == null || chefServiceLocationList.isEmpty()) {
            return Collections.emptyList();
        }
        return chefServiceLocationList.stream()
                .map(this::toChefServiceLocationVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    @Override
    public ChefServiceLocationVO getCurrentChefServiceLocationById(Long id) {
        return toChefServiceLocationVO(getOwnedLocation(id));
    }

    /**
     * 新建一条业务数据。
     * 这个方法用于把前端提交的新信息正式写入数据库。
     * 它会先做必要的检查和组装，再保存数据，最后返回新建后的结果。
     */
    @Override
    public ChefServiceLocationVO createCurrentChefServiceLocation(ChefServiceLocationCreateDTO chefServiceLocationCreateDTO) {
        Long chefId = requireCurrentChefId();
        requireChefExists(chefId);
        validateLocationFields(
                chefServiceLocationCreateDTO.getProvince(),
                chefServiceLocationCreateDTO.getCity(),
                chefServiceLocationCreateDTO.getDistrict(),
                chefServiceLocationCreateDTO.getDetailAddress(),
                chefServiceLocationCreateDTO.getLongitude(),
                chefServiceLocationCreateDTO.getLatitude()
        );

        LocalDateTime now = LocalDateTime.now();
        ChefServiceLocation chefServiceLocation = ChefServiceLocation.builder()
                .chefId(chefId)
                .locationName(normalizeText(chefServiceLocationCreateDTO.getLocationName()))
                .province(normalizeText(chefServiceLocationCreateDTO.getProvince()))
                .city(normalizeText(chefServiceLocationCreateDTO.getCity()))
                .district(normalizeText(chefServiceLocationCreateDTO.getDistrict()))
                .town(normalizeText(chefServiceLocationCreateDTO.getTown()))
                .detailAddress(normalizeText(chefServiceLocationCreateDTO.getDetailAddress()))
                .longitude(chefServiceLocationCreateDTO.getLongitude())
                .latitude(chefServiceLocationCreateDTO.getLatitude())
                .isActive(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        int rows = chefServiceLocationMapper.insert(chefServiceLocation);
        if (rows <= 0) {
            return null;
        }
        return toChefServiceLocationVO(chefServiceLocationMapper.selectById(chefServiceLocation.getId()));
    }

    /**
     * 修改一条已有的业务数据。
     * 这个方法用于更新详情信息、状态或某些可编辑字段。
     * 它会先查出原始数据，再把新值写进去，最后保存并返回更新后的结果。
     */
    @Override
    public ChefServiceLocationVO updateCurrentChefServiceLocation(Long id, ChefServiceLocationUpdateDTO chefServiceLocationUpdateDTO) {
        validateLocationFields(
                chefServiceLocationUpdateDTO.getProvince(),
                chefServiceLocationUpdateDTO.getCity(),
                chefServiceLocationUpdateDTO.getDistrict(),
                chefServiceLocationUpdateDTO.getDetailAddress(),
                chefServiceLocationUpdateDTO.getLongitude(),
                chefServiceLocationUpdateDTO.getLatitude()
        );

        ChefServiceLocation existingLocation = getOwnedLocation(id);
        existingLocation.setLocationName(normalizeText(chefServiceLocationUpdateDTO.getLocationName()));
        existingLocation.setProvince(normalizeText(chefServiceLocationUpdateDTO.getProvince()));
        existingLocation.setCity(normalizeText(chefServiceLocationUpdateDTO.getCity()));
        existingLocation.setDistrict(normalizeText(chefServiceLocationUpdateDTO.getDistrict()));
        existingLocation.setTown(normalizeText(chefServiceLocationUpdateDTO.getTown()));
        existingLocation.setDetailAddress(normalizeText(chefServiceLocationUpdateDTO.getDetailAddress()));
        existingLocation.setLongitude(chefServiceLocationUpdateDTO.getLongitude());
        existingLocation.setLatitude(chefServiceLocationUpdateDTO.getLatitude());
        existingLocation.setUpdatedAt(LocalDateTime.now());

        int rows = chefServiceLocationMapper.updateById(existingLocation);
        if (rows <= 0) {
            return null;
        }
        return toChefServiceLocationVO(chefServiceLocationMapper.selectById(id));
    }

    /**
     * 删除一条不再需要的数据。
     * 这个方法主要用来清理记录，避免无效数据继续留在系统里。
     * 它通常会先查询要删的数据，确认没问题后再执行删除。
     */
    @Override
    public ChefServiceLocationVO deleteCurrentChefServiceLocation(Long id) {
        ChefServiceLocation existingLocation = getOwnedLocation(id);
        int rows = chefServiceLocationMapper.deleteById(id, existingLocation.getChefId());
        if (rows <= 0) {
            return null;
        }
        return toChefServiceLocationVO(existingLocation);
    }

    /**
     * 处理 activateCurrentChefServiceLocation 这个方法对应的业务逻辑。
     * 这个方法主要是把当前模块里的某一段独立工作单独拆出来，让主流程更清楚。
     * 它会围绕自己的职责去查询数据、处理规则，最后返回结果或更新状态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChefServiceLocationVO activateCurrentChefServiceLocation(Long id) {
        ChefServiceLocation existingLocation = getOwnedLocation(id);
        LocalDateTime now = LocalDateTime.now();
        chefServiceLocationMapper.resetActiveByChefId(existingLocation.getChefId(), now);
        int rows = chefServiceLocationMapper.activateById(id, existingLocation.getChefId(), now);
        if (rows <= 0) {
            return null;
        }
        return toChefServiceLocationVO(chefServiceLocationMapper.selectById(id));
    }

    /**
     * 查询一组符合条件的列表数据。
     * 这个方法主要给列表页面或管理页面使用，让调用方可以一次拿到需要的数据。
     * 它会根据传入的条件查数据，如果需要的话还会把结果转成接口要返回的对象。
     */
    @Override
    public List<ChefServiceLocationVO> getChefServiceLocationListByChefId(Long chefId) {
        requireChefExists(chefId);
        List<ChefServiceLocation> chefServiceLocationList = chefServiceLocationMapper.selectListByChefId(chefId);
        if (chefServiceLocationList == null || chefServiceLocationList.isEmpty()) {
            return Collections.emptyList();
        }
        return chefServiceLocationList.stream()
                .map(this::toChefServiceLocationVO)
                .collect(Collectors.toList());
    }

    /**
     * 查出当前业务必须要用的数据。
     * 这个方法用于把“先查数据，找不到就报错”这类逻辑集中到一起。
     * 它会根据 id 或当前登录信息去查记录，如果查不到或不符合条件，就直接抛出异常。
     */
    private Long requireCurrentChefId() {
        Long chefId = LoginUserContext.getChefId();
        if (chefId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "unauthorized");
        }
        return chefId;
    }

    /**
     * 查出当前业务必须要用的数据。
     * 这个方法用于把“先查数据，找不到就报错”这类逻辑集中到一起。
     * 它会根据 id 或当前登录信息去查记录，如果查不到或不符合条件，就直接抛出异常。
     */
    private void requireChefExists(Long chefId) {
        Chef chef = chefMapper.selectById(chefId);
        if (chef == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "chef not found");
        }
    }

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    private ChefServiceLocation getOwnedLocation(Long id) {
        Long chefId = requireCurrentChefId();
        requireChefExists(chefId);
        ChefServiceLocation chefServiceLocation = chefServiceLocationMapper.selectByChefIdAndId(chefId, id);
        if (chefServiceLocation == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND, "service location not found");
        }
        return chefServiceLocation;
    }

    /**
     * 检查当前传入的参数或业务状态是否合法。
     * 这个方法的作用，是把不合条件的情况尽早拦住，不让错误数据继续往下执行。
     * 它会根据规则逐项检查参数或状态，只要发现不满足条件，就直接抛出异常。
     */
    private void validateLocationFields(String province,
                                        String city,
                                        String district,
                                        String detailAddress,
                                        BigDecimal longitude,
                                        BigDecimal latitude) {
        if (!StringUtils.hasText(province)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "province 不能为空");
        }
        if (!StringUtils.hasText(city)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "city 不能为空");
        }
        if (!StringUtils.hasText(district)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "district 不能为空");
        }
        if (!StringUtils.hasText(detailAddress)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "detailAddress 不能为空");
        }
        if (longitude == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "longitude 不能为空");
        }
        if (latitude == null) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "latitude 不能为空");
        }
    }

    /**
     * 把输入值整理成统一的格式。
     * 这个方法可以避免因为大小写、空格或不同写法导致后面的业务判断出错。
     * 它通常会先做 trim，再统一大小写，或者转成系统里约定好的标准值。
     */
    private String normalizeText(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    /**
     * 把数据对象转成接口要返回的格式。
     * 这个方法让主流程不用反复写字段赋值逻辑，代码会更整洁。
     * 它会从实体或中间对象里取出需要的字段，然后组装 VO 或其他返回对象。
     */
    private ChefServiceLocationVO toChefServiceLocationVO(ChefServiceLocation chefServiceLocation) {
        if (chefServiceLocation == null) {
            return null;
        }
        return ChefServiceLocationVO.builder()
                .id(chefServiceLocation.getId())
                .chefId(chefServiceLocation.getChefId())
                .locationName(chefServiceLocation.getLocationName())
                .province(chefServiceLocation.getProvince())
                .city(chefServiceLocation.getCity())
                .district(chefServiceLocation.getDistrict())
                .town(chefServiceLocation.getTown())
                .detailAddress(chefServiceLocation.getDetailAddress())
                .longitude(chefServiceLocation.getLongitude())
                .latitude(chefServiceLocation.getLatitude())
                .isActive(chefServiceLocation.getIsActive())
                .createdAt(chefServiceLocation.getCreatedAt())
                .updatedAt(chefServiceLocation.getUpdatedAt())
                .build();
    }
}
