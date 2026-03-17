package com.homechef.homechefsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homechef.homechefsystem.annotation.RequireAdmin;
import com.homechef.homechefsystem.annotation.RequireLogin;
import com.homechef.homechefsystem.common.result.Result;
import com.homechef.homechefsystem.utils.JwtUtil;
import com.homechef.homechefsystem.utils.LoginUserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireAdmin requireAdmin = getRequireAdmin(handlerMethod);
        RequireLogin requireLogin = getRequireLogin(handlerMethod);
        if (requireAdmin == null && requireLogin == null) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        String token = jwtUtil.extractToken(authorization);
        if (token == null || !jwtUtil.isTokenValid(token)) {
            writeErrorResponse(response, 401, "unauthorized");
            return false;
        }

        String userType = jwtUtil.getUserType(token);
        Long userId = jwtUtil.getUserId(token);
        Long adminId = jwtUtil.getAdminId(token);

        LoginUserContext.set(LoginUserContext.LoginUser.builder()
                .userId(userId)
                .adminId(adminId)
                .userType(userType)
                .build());

        if (requireAdmin != null && !JwtUtil.USER_TYPE_ADMIN.equals(userType)) {
            LoginUserContext.clear();
            writeErrorResponse(response, 403, "forbidden");
            return false;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserContext.clear();
    }

    private RequireLogin getRequireLogin(HandlerMethod handlerMethod) {
        RequireLogin requireLogin = handlerMethod.getMethodAnnotation(RequireLogin.class);
        if (requireLogin != null) {
            return requireLogin;
        }
        return handlerMethod.getBeanType().getAnnotation(RequireLogin.class);
    }

    private RequireAdmin getRequireAdmin(HandlerMethod handlerMethod) {
        RequireAdmin requireAdmin = handlerMethod.getMethodAnnotation(RequireAdmin.class);
        if (requireAdmin != null) {
            return requireAdmin;
        }
        return handlerMethod.getBeanType().getAnnotation(RequireAdmin.class);
    }

    private void writeErrorResponse(HttpServletResponse response, Integer code, String message) throws Exception {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(Result.error(code, message)));
    }
}
