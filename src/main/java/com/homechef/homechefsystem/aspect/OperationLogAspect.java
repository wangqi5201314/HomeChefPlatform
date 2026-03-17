package com.homechef.homechefsystem.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homechef.homechefsystem.common.annotation.OperationLog;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.service.OperationLogService;
import com.homechef.homechefsystem.utils.LoginUserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OperationLogService operationLogService;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        Object result = null;
        Integer status = 1;
        String responseData = null;
        Throwable throwable = null;

        try {
            result = joinPoint.proceed();
            responseData = toJsonString(result);
            if (result instanceof Result<?> resultData && resultData.getCode() != null && resultData.getCode() != 200) {
                status = 0;
            }
            return result;
        } catch (Throwable e) {
            status = 0;
            throwable = e;
            responseData = e.getMessage();
            throw e;
        } finally {
            try {
                saveOperationLog(joinPoint, operationLog, status, responseData);
            } catch (Exception ignored) {
                if (throwable == null) {
                    // no-op
                }
            }
        }
    }

    private void saveOperationLog(ProceedingJoinPoint joinPoint,
                                  OperationLog operationLogAnnotation,
                                  Integer status,
                                  String responseData) {
        HttpServletRequest request = getCurrentRequest();
        String operatorType = LoginUserContext.getUserType();
        Long operatorId = LoginUserContext.isAdmin() ? LoginUserContext.getAdminId() : LoginUserContext.getUserId();

        com.homechef.homechefsystem.entity.OperationLog operationLog = com.homechef.homechefsystem.entity.OperationLog.builder()
                .operatorType(operatorType == null ? "SYSTEM" : operatorType)
                .operatorId(operatorId)
                .module(operationLogAnnotation.module())
                .operation(operationLogAnnotation.operation())
                .requestMethod(request == null ? null : request.getMethod())
                .requestUri(request == null ? null : request.getRequestURI())
                .requestParams(buildRequestParams(joinPoint.getArgs()))
                .responseData(responseData)
                .ip(getIp(request))
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();

        operationLogService.save(operationLog);
    }

    private HttpServletRequest getCurrentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }

    private String getIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String buildRequestParams(Object[] args) {
        List<Object> validArgs = new ArrayList<>();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof HttpServletRequest
                        || arg instanceof HttpServletResponse
                        || arg instanceof MultipartFile) {
                    continue;
                }
                validArgs.add(arg);
            }
        }
        return toJsonString(validArgs);
    }

    private String toJsonString(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            return String.valueOf(data);
        }
    }
}
