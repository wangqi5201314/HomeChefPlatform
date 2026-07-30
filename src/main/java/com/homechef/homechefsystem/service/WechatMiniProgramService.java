package com.homechef.homechefsystem.service;

public interface WechatMiniProgramService {

    /**
     * 根据微信登录 code 换取 session 信息。
     */
    WechatLoginInfo code2Session(String code);

    record WechatLoginInfo(String openid, String sessionKey, String unionid) {
    }
}
