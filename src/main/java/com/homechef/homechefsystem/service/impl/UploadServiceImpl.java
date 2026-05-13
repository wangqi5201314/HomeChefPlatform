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
     * 上传一张图片到 OSS 存储。
     * 这个方法给系统里的头像上传、材料上传等场景使用，负责生成新文件名并返回访问地址。
     * 它会先检查文件是否合法，再生成 UUID 文件名和存储路径，接着上传到 OSS，最后拼出对外 URL。
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
     * 检查当前传入的参数或业务状态是否合法。
     * 这个方法的作用，是把不合条件的情况尽早拦住，不让错误数据继续往下执行。
     * 它会根据规则逐项检查参数或状态，只要发现不满足条件，就直接抛出异常。
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
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
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
     * 生成文件在 OSS 中保存时使用的完整路径。
     * 这个方法的作用，是把配置里的上传目录前缀和文件名拼接起来。
     * 它会先处理前缀里多余的斜杠，如果没有配置前缀，就直接返回文件名本身。
     */
    private String buildObjectKey(String fileName) {
        String prefix = trimSlashes(ossProperties.getUploadPrefix());
        if (!StringUtils.hasText(prefix)) {
            return fileName;
        }
        return prefix + "/" + fileName;
    }

    /**
     * 构建一个后续会被重复使用的中间结果。
     * 这个方法主要是为了把主流程里的细节拆出去，让主流程更容易看。
     * 它会根据当前需要把集合、映射、路径、文本或比较器等内容先准备好。
     */
    private String buildFileUrl(String objectKey) {
        if (StringUtils.hasText(ossProperties.getCustomDomain())) {
            return trimTrailingSlash(ossProperties.getCustomDomain()) + "/" + objectKey;
        }
        return "https://" + ossProperties.getBucketName() + "." + trimProtocol(ossProperties.getEndpoint()) + "/" + objectKey;
    }

    /**
     * 查询一条详细数据。
     * 这个方法主要用在详情页面或后续业务处理前的数据准备。
     * 它会根据 id、当前登录人或其他条件去查数据，找到后再转成返回给前端的格式。
     */
    private String getExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "invalid file name");
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 根据当前条件解析出最终要使用的值。
     * 这个方法主要用来把多个输入整理成一个标准结果。
     * 它会结合参数和默认规则做判断，最后返回真正要用的内容。
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
     * 把字符串前后多余的部分清理掉。
     * 这个方法主要用来处理路径、URL 或前缀，避免拼接后格式不对。
     * 它会按当前方法的规则去掉多余斜杠、协议头或尾部分隔符。
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
     * 把字符串前后多余的部分清理掉。
     * 这个方法主要用来处理路径、URL 或前缀，避免拼接后格式不对。
     * 它会按当前方法的规则去掉多余斜杠、协议头或尾部分隔符。
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
     * 把字符串前后多余的部分清理掉。
     * 这个方法主要用来处理路径、URL 或前缀，避免拼接后格式不对。
     * 它会按当前方法的规则去掉多余斜杠、协议头或尾部分隔符。
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
