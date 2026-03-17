package com.homechef.homechefsystem.controller.admin;

import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.ChefCertificationAuditDTO;
import com.homechef.homechefsystem.dto.ChefCertificationQueryDTO;
import com.homechef.homechefsystem.service.ChefCertificationService;
import com.homechef.homechefsystem.vo.ChefCertificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/chef/certification")
@RequiredArgsConstructor
public class AdminChefCertificationController {

    private final ChefCertificationService chefCertificationService;

    @GetMapping("/list")
    public Result<List<ChefCertificationVO>> getList(ChefCertificationQueryDTO queryDTO) {
        return Result.success(chefCertificationService.getList(queryDTO));
    }

    @PostMapping("/{id}/audit")
    public Result<ChefCertificationVO> auditById(@PathVariable Long id,
                                                 @RequestBody ChefCertificationAuditDTO chefCertificationAuditDTO) {
        Integer auditStatus = chefCertificationAuditDTO.getAuditStatus();
        if (auditStatus == null || (!Integer.valueOf(1).equals(auditStatus) && !Integer.valueOf(2).equals(auditStatus))) {
            return Result.error(400, "audit status invalid");
        }

        ChefCertificationVO chefCertificationVO = chefCertificationService.auditById(id, chefCertificationAuditDTO);
        if (chefCertificationVO == null) {
            return Result.error(404, "certification not found");
        }
        return Result.success(chefCertificationVO);
    }
}
