package com.homechef.homechefsystem.controller.chef;

import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.dto.ChefCertificationSubmitDTO;
import com.homechef.homechefsystem.service.ChefCertificationService;
import com.homechef.homechefsystem.vo.ChefCertificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chef/certification")
@RequiredArgsConstructor
public class ChefCertificationController {

    private final ChefCertificationService chefCertificationService;

    @PostMapping("/submit")
    public Result<ChefCertificationVO> submit(@RequestBody ChefCertificationSubmitDTO chefCertificationSubmitDTO) {
        if (!chefCertificationService.chefExists(chefCertificationSubmitDTO.getChefId())) {
            return Result.error(404, "chef not found");
        }

        ChefCertificationVO chefCertificationVO = chefCertificationService.submit(chefCertificationSubmitDTO);
        if (chefCertificationVO == null) {
            return Result.error(500, "submit certification failed");
        }
        return Result.success(chefCertificationVO);
    }

    @GetMapping("/{chefId}")
    public Result<ChefCertificationVO> getByChefId(@PathVariable Long chefId) {
        ChefCertificationVO chefCertificationVO = chefCertificationService.getByChefId(chefId);
        if (chefCertificationVO == null) {
            return Result.error(404, "certification not found");
        }
        return Result.success(chefCertificationVO);
    }
}
