package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

    /**
     * 上传图片并返回文件信息。
     */
    FileUploadVO uploadImage(MultipartFile file);
}
