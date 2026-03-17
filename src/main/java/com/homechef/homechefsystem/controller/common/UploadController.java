package com.homechef.homechefsystem.controller.common;

import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.service.UploadService;
import com.homechef.homechefsystem.vo.FileUploadVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@Tag(name = "通用上传接口")
public class UploadController {

    private final UploadService uploadService;

    @Operation(summary = "上传图片")
    @PostMapping("/image")
    public Result<FileUploadVO> uploadImage(@RequestParam("file") MultipartFile file) {
        FileUploadVO fileUploadVO = uploadService.uploadImage(file);
        if (fileUploadVO == null) {
            return Result.error(400, "upload image failed");
        }
        return Result.success(fileUploadVO);
    }
}
