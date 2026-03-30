package com.homechef.homechefsystem.controller.admin;

import com.homechef.homechefsystem.annotation.RequireAdmin;
import com.homechef.homechefsystem.common.annotation.OperationLog;
import com.homechef.homechefsystem.common.enums.ChefCertStatusEnum;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.ChefCertificationAuditDTO;
import com.homechef.homechefsystem.dto.ChefCertificationQueryDTO;
import com.homechef.homechefsystem.service.ChefCertificationService;
import com.homechef.homechefsystem.vo.ChefCertificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/chef")
@RequiredArgsConstructor
@Tag(name = "后台厨师认证接口")
public class AdminChefCertificationController {

    private final ChefCertificationService chefCertificationService;

    @RequireAdmin
    @Operation(summary = "查询厨师认证列表")
    @GetMapping("/certifications")
    public Result<List<ChefCertificationVO>> getCertificationList(ChefCertificationQueryDTO queryDTO) {
        return Result.success(chefCertificationService.getList(queryDTO));
    }

    @RequireAdmin
    @Operation(summary = "查询厨师认证列表")
    @GetMapping("/certification/list")
    public Result<List<ChefCertificationVO>> getList(ChefCertificationQueryDTO queryDTO) {
        return Result.success(chefCertificationService.getList(queryDTO));
    }

    @RequireAdmin
    @OperationLog(module = "AdminChefCertification", operation = "Audit Chef Certification")
    @Operation(summary = "审核厨师认证")
    @PostMapping("/certification/{id}/audit")
    public Result<ChefCertificationVO> auditById(@PathVariable Long id,
                                                 @RequestBody ChefCertificationAuditDTO chefCertificationAuditDTO) {
        Integer auditStatus = chefCertificationAuditDTO.getAuditStatus();
        if (!ChefCertStatusEnum.isAuditResult(auditStatus)) {
            return Result.error(400, "auditStatus 取值非法，只能为审核通过或审核拒绝");
        }

        ChefCertificationVO chefCertificationVO = chefCertificationService.auditById(id, chefCertificationAuditDTO);
        if (chefCertificationVO == null) {
            return Result.error(404, "certification not found");
        }
        return Result.success(chefCertificationVO);
    }
}
