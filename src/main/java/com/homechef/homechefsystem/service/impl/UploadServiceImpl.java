package com.homechef.homechefsystem.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.config.OssProperties;
import com.homechef.homechefsystem.service.UploadService;
import com.homechef.homechefsystem.vo.FileUploadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private static final Set<String> DEFAULT_ALLOWED_TYPES = Set.of("jpg", "jpeg", "png", "webp");

    private final OSS ossClient;
    private final OssProperties ossProperties;

    @Override
    /**
     * 上传图片并返回可访问的文件信息。
     */
    public FileUploadVO uploadImage(MultipartFile file) {
        validateFile(file);

        String originalFileName = file.getOriginalFilename();
        String extension = getExtension(originalFileName);
        String fileName = UUID.randomUUID() + "." + extension;
        String objectKey = buildObjectKey(fileName);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(resolveContentType(extension, file.getContentType()));

        try {
            ossClient.putObject(ossProperties.getBucketName(), objectKey, file.getInputStream(), metadata);
        } catch (IOException e) {
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "upload image failed");
        }

        return FileUploadVO.builder()
                .fileName(fileName)
                .originalFileName(originalFileName)
                .fileUrl(buildFileUrl(objectKey))
                .fileSize(file.getSize())
                .contentType(metadata.getContentType())
                .build();
    }

    /**
     * 校验输入参数或业务状态是否合法。
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "file is empty");
        }

        long maxSize = Math.max(1, ossProperties.getMaxSizeMb()) * 1024L * 1024L;
        if (file.getSize() > maxSize) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "file size exceeds limit");
        }

        String extension = getExtension(file.getOriginalFilename());
        if (!getAllowedTypes().contains(extension)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "file type not supported");
        }
    }

    /**
     * 获取当前允许上传的文件类型集合。
     */
    private Set<String> getAllowedTypes() {
        if (!StringUtils.hasText(ossProperties.getAllowedTypes())) {
            return DEFAULT_ALLOWED_TYPES;
        }
        return Arrays.stream(ossProperties.getAllowedTypes().split(","))
                .map(item -> item == null ? "" : item.trim().toLowerCase(Locale.ROOT))
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 生成上传到对象存储时使用的对象路径。
     */
    private String buildObjectKey(String fileName) {
        String prefix = trimSlashes(ossProperties.getUploadPrefix());
        if (!StringUtils.hasText(prefix)) {
            return fileName;
        }
        return prefix + "/" + fileName;
    }

    /**
     * 根据对象路径拼接文件访问地址。
     */
    private String buildFileUrl(String objectKey) {
        if (StringUtils.hasText(ossProperties.getCustomDomain())) {
            return trimTrailingSlash(ossProperties.getCustomDomain()) + "/" + objectKey;
        }
        return "https://" + ossProperties.getBucketName() + "." + trimProtocol(ossProperties.getEndpoint()) + "/" + objectKey;
    }

    /**
     * 提取文件扩展名。
     */
    private String getExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "invalid file name");
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 确定上传文件的内容类型。
     */
    private String resolveContentType(String extension, String contentType) {
        if (StringUtils.hasText(contentType)) {
            return contentType;
        }
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    /**
     * 去除字符串首尾多余的斜杠。
     */
    private String trimSlashes(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String result = value.trim();
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * 去除字符串末尾的斜杠。
     */
    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * 去除地址中的协议头。
     */
    private String trimProtocol(String endpoint) {
        String result = endpoint.trim();
        if (result.startsWith("https://")) {
            return result.substring(8);
        }
        if (result.startsWith("http://")) {
            return result.substring(7);
        }
        return result;
    }
}
