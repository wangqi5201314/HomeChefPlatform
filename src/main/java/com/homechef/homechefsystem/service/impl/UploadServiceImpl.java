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

    /**
     * 方法说明：校验并上传图片文件到对象存储，随后返回可访问地址。
     * 主要作用：它为头像、认证材料等上传场景提供统一入口，避免不同业务各自处理文件校验和 OSS 路径拼接。
     * 实现逻辑：方法会先检查文件是否为空、格式是否合法，再生成对象键并上传到 OSS，最后拼接出外部访问 URL 返回给调用方。
     */
    @Override
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
     * 方法说明：校验当前业务输入或状态是否满足执行条件。
     * 主要作用：它用于把 文件上传服务实现 中的前置规则集中收口，避免核心流程夹杂过多重复的条件判断。
     * 实现逻辑：实现逻辑会逐项检查关键字段、状态或业务约束，一旦发现不满足条件的情况就立即抛出业务异常阻断流程。
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
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 文件上传服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
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
     * 方法说明：构建当前业务流程后续需要复用的中间结果。
     * 主要作用：这个辅助方法把 文件上传服务实现 中重复使用的数据结构提前整理好，减少主流程中的重复计算和分支判断。
     * 实现逻辑：实现逻辑通常会对输入参数做空值保护，再根据业务规则拼装映射、集合、文本或比较器，供后续步骤直接复用。
     */
    private String buildObjectKey(String fileName) {
        String prefix = trimSlashes(ossProperties.getUploadPrefix());
        if (!StringUtils.hasText(prefix)) {
            return fileName;
        }
        return prefix + "/" + fileName;
    }

    /**
     * 方法说明：构建当前业务流程后续需要复用的中间结果。
     * 主要作用：这个辅助方法把 文件上传服务实现 中重复使用的数据结构提前整理好，减少主流程中的重复计算和分支判断。
     * 实现逻辑：实现逻辑通常会对输入参数做空值保护，再根据业务规则拼装映射、集合、文本或比较器，供后续步骤直接复用。
     */
    private String buildFileUrl(String objectKey) {
        if (StringUtils.hasText(ossProperties.getCustomDomain())) {
            return trimTrailingSlash(ossProperties.getCustomDomain()) + "/" + objectKey;
        }
        return "https://" + ossProperties.getBucketName() + "." + trimProtocol(ossProperties.getEndpoint()) + "/" + objectKey;
    }

    /**
     * 方法说明：查询一条当前业务所需的详情数据。
     * 主要作用：该方法用于 文件上传服务实现 中的详情展示、状态流转前校验或后续业务处理前的数据加载。
     * 实现逻辑：实现时会根据主键、关联键或当前登录身份查出目标记录，再按需要转换成 VO，必要时会补充关联字段或做存在性校验。
     */
    private String getExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "invalid file name");
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 方法说明：在 文件上传服务实现 中处理 resolveContentType 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
     * 方法说明：在 文件上传服务实现 中处理 trimSlashes 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
     * 方法说明：在 文件上传服务实现 中处理 trimTrailingSlash 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
     * 方法说明：在 文件上传服务实现 中处理 trimProtocol 相关的业务逻辑。
     * 主要作用：该方法用于承接当前模块中的一个独立职责点，帮助主流程保持清晰并减少重复代码。
     * 实现逻辑：实现逻辑会围绕当前方法职责完成必要的数据查询、规则判断、字段加工或结果返回，并在发现异常场景时及时中断流程。
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
