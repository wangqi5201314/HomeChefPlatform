package com.homechef.homechefsystem.service;

import com.homechef.homechefsystem.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

    FileUploadVO uploadImage(MultipartFile file);
}
