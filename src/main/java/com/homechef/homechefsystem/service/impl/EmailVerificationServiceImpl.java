package com.homechef.homechefsystem.service.impl;

import com.homechef.homechefsystem.common.enums.ResultCodeEnum;
import com.homechef.homechefsystem.common.exception.BusinessException;
import com.homechef.homechefsystem.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final String CODE_KEY_PREFIX = "homechef:auth:email:code:";
    private static final String COOLDOWN_KEY_PREFIX = "homechef:auth:email:cooldown:";
    private static final String ATTEMPTS_KEY_PREFIX = "homechef:auth:email:attempts:";
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN_TTL = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate stringRedisTemplate;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Override
    public void sendLoginCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        String cooldownKey = COOLDOWN_KEY_PREFIX + normalizedEmail;

        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "1", COOLDOWN_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(ResultCodeEnum.FAIL, "please wait before requesting another code");
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(normalizedEmail);
            message.setSubject("HomeChef login verification code");
            message.setText("Your HomeChef login verification code is " + code
                    + ". It will expire in 1 minutes. If you did " +
                    "not request this code, ignore this email.");
            mailSender.send(message);

            stringRedisTemplate.opsForValue().set(CODE_KEY_PREFIX + normalizedEmail, code, CODE_TTL);
            stringRedisTemplate.delete(ATTEMPTS_KEY_PREFIX + normalizedEmail);
        } catch (Exception exception) {
            stringRedisTemplate.delete(cooldownKey);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR, "failed to send email code");
        }
    }

    @Override
    public void verifyLoginCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        String codeKey = CODE_KEY_PREFIX + normalizedEmail;
        String attemptsKey = ATTEMPTS_KEY_PREFIX + normalizedEmail;
        String expectedCode = stringRedisTemplate.opsForValue().get(codeKey);

        if (!StringUtils.hasText(expectedCode)) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "code is expired or invalid");
        }
        if (expectedCode.equals(code)) {
            stringRedisTemplate.delete(codeKey);
            stringRedisTemplate.delete(attemptsKey);
            return;
        }

        Long attempts = stringRedisTemplate.opsForValue().increment(attemptsKey);
        stringRedisTemplate.expire(attemptsKey, CODE_TTL);
        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            stringRedisTemplate.delete(codeKey);
            stringRedisTemplate.delete(attemptsKey);
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "too many invalid code attempts");
        }
        throw new BusinessException(ResultCodeEnum.UNAUTHORIZED, "code is invalid");
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR, "email can not be blank");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
