package com.homechef.homechefsystem.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class LoginUserContext {

    private static final ThreadLocal<LoginUser> LOGIN_USER_HOLDER = new ThreadLocal<>();

    private LoginUserContext() {
    }

    public static void set(LoginUser loginUser) {
        LOGIN_USER_HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return LOGIN_USER_HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser loginUser = get();
        return loginUser == null ? null : loginUser.getUserId();
    }

    public static Long getAdminId() {
        LoginUser loginUser = get();
        return loginUser == null ? null : loginUser.getAdminId();
    }

    public static Long getChefId() {
        LoginUser loginUser = get();
        return loginUser == null ? null : loginUser.getChefId();
    }

    public static String getUserType() {
        LoginUser loginUser = get();
        return loginUser == null ? null : loginUser.getUserType();
    }

    public static boolean isAdmin() {
        return JwtUtil.USER_TYPE_ADMIN.equals(getUserType());
    }

    public static boolean isChef() {
        return JwtUtil.USER_TYPE_CHEF.equals(getUserType());
    }

    public static void clear() {
        LOGIN_USER_HOLDER.remove();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginUser {

        private Long userId;

        private Long adminId;

        private Long chefId;

        private String userType;
    }
}
