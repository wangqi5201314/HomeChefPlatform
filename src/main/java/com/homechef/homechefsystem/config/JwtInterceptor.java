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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    /**
     * 拦截器中直接写 JSON 响应时使用。
     * 例如 token 无效或权限不足时，不再进入 Controller，而是直接返回统一 Result 结构。
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final JwtUtil jwtUtil;

    /**
     * Controller 方法执行前触发。
     *
     * 核心职责：
     * 1. 判断当前接口是否需要登录或管理员权限
     * 2. 从 Authorization 请求头中提取 JWT
     * 3. 校验 token 是否有效、是否过期
     * 4. 解析 userId/adminId/chefId/userType，并写入 LoginUserContext
     * 5. 对 @RequireAdmin 接口额外校验 ADMIN 身份
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 浏览器跨域预检请求使用 OPTIONS 方法，不应该被登录校验拦截。
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        // 静态资源、错误页等请求可能不是 Controller 方法，直接放行。
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 支持方法级注解，也支持 Controller 类级注解。
        // 只要方法或类上存在 @RequireLogin / @RequireAdmin，就需要进行 token 校验。
        RequireAdmin requireAdmin = getRequireAdmin(handlerMethod);
        RequireLogin requireLogin = getRequireLogin(handlerMethod);
        if (requireAdmin == null && requireLogin == null) {
            return true;
        }

        // 前端请求头标准格式：Authorization: Bearer token
        // JwtUtil.extractToken 只负责去掉 Bearer 前缀并取出真正的 token 字符串。
        String authorization = request.getHeader("Authorization");
        String token = jwtUtil.extractToken(authorization);
        if (token == null || !jwtUtil.isTokenValid(token)) {
            // token 不存在、签名错误或已过期，都统一返回 401。
            writeErrorResponse(response, 401, "unauthorized");
            return false;
        }

        // 从 token payload 中解析三端身份信息。
        // 普通用户 token 只有 userId，管理员 token 只有 adminId，厨师 token 只有 chefId。
        String userType = jwtUtil.getUserType(token);
        Long userId = jwtUtil.getUserId(token);
        Long adminId = jwtUtil.getAdminId(token);
        Long chefId = jwtUtil.getChefId(token);

        // 将当前请求的登录身份保存到 ThreadLocal。
        // 后续 Service 层可以通过 LoginUserContext.getUserId()/getChefId() 获取当前登录人。
        LoginUserContext.set(LoginUserContext.LoginUser.builder()
                .userId(userId)
                .adminId(adminId)
                .chefId(chefId)
                .userType(userType)
                .build());

        // @RequireAdmin 比 @RequireLogin 更严格：不仅要有合法 token，还必须是 ADMIN 类型。
        if (requireAdmin != null && !JwtUtil.USER_TYPE_ADMIN.equals(userType)) {
            // 当前请求不会继续执行，提前清理上下文，避免后续复用线程时残留登录信息。
            LoginUserContext.clear();
            writeErrorResponse(response, 403, "forbidden");
            return false;
        }

        return true;
    }

    /**
     * Controller 执行完成后触发。
     *
     * LoginUserContext 底层通常是 ThreadLocal。
     * Web 容器线程会复用，如果不清理，下一次请求可能错误拿到上一次请求的登录人信息。
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserContext.clear();
    }

    /**
     * 获取 @RequireLogin 注解。
     * 优先读取方法上的注解；如果方法上没有，再读取 Controller 类上的注解。
     */
    private RequireLogin getRequireLogin(HandlerMethod handlerMethod) {
        RequireLogin requireLogin = handlerMethod.getMethodAnnotation(RequireLogin.class);
        if (requireLogin != null) {
            return requireLogin;
        }
        return handlerMethod.getBeanType().getAnnotation(RequireLogin.class);
    }

    /**
     * 获取 @RequireAdmin 注解。
     * @RequireAdmin 表示接口只能管理员访问，普通用户和厨师 token 即使有效也不能访问。
     */
    private RequireAdmin getRequireAdmin(HandlerMethod handlerMethod) {
        RequireAdmin requireAdmin = handlerMethod.getMethodAnnotation(RequireAdmin.class);
        if (requireAdmin != null) {
            return requireAdmin;
        }
        return handlerMethod.getBeanType().getAnnotation(RequireAdmin.class);
    }

    /**
     * 拦截器中不能直接返回 Result 对象，所以这里手动把 Result 序列化为 JSON 写入响应体。
     */
    private void writeErrorResponse(HttpServletResponse response, Integer code, String message) throws Exception {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(Result.error(code, message)));
    }
}
