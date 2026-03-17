package com.homechef.homechefsystem.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadVO {

    private String fileName;

    private String originalFileName;

    private String fileUrl;

    private Long fileSize;

    private String contentType;
}
