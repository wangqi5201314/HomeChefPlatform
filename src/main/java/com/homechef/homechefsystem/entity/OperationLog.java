package com.homechef.homechefsystem.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog {

    private Long id;

    private String operatorType;

    private Long operatorId;

    private String module;

    private String operation;

    private String requestMethod;

    private String requestUri;

    private String requestParams;

    private String responseData;

    private String ip;

    private Integer status;

    private LocalDateTime createdAt;
}
