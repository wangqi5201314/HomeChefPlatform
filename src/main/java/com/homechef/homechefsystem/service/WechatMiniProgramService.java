package com.homechef.homechefsystem.service;

public interface WechatMiniProgramService {

    WechatLoginInfo code2Session(String code);

    record WechatLoginInfo(String openid, String sessionKey, String unionid) {
    }
}
