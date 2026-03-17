package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.service.UploadService;
import com.homechef.homechefsystem.vo.FileUploadVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadServiceImpl implements UploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final String UPLOAD_DIR = "uploads/images";

    @Override
    public FileUploadVO uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFileName = file.getOriginalFilename();
        String extension = getExtension(originalFileName);
        String contentType = file.getContentType();

        if (!ALLOWED_EXTENSIONS.contains(extension) || contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return null;
        }

        String fileName = UUID.randomUUID() + "." + extension;
        Path uploadPath = Paths.get(System.getProperty("user.dir"), UPLOAD_DIR);

        try {
            Files.createDirectories(uploadPath);
            Files.copy(file.getInputStream(), uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return null;
        }

        return FileUploadVO.builder()
                .fileName(fileName)
                .originalFileName(originalFileName)
                .fileUrl("/uploads/images/" + fileName)
                .fileSize(file.getSize())
                .contentType(contentType)
                .build();
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
