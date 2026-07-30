package com.homechef.homechefsystem.service;

public interface EmailVerificationService {

    void sendLoginCode(String email);

    void verifyLoginCode(String email, String code);
}
