package com.mpq.service.Impl;

import com.mpq.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final String CODE_PREFIX = "email:code:";
    private static final int CODE_EXPIRE_MINUTES = 5;

    @Override
    public void sendVerificationCode(String email) {
        String code = generateRandomCode(6);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("登录验证码");
        message.setText("您的登录验证码是：" + code + "，有效期为5分钟，请勿泄露给他人。");
        mailSender.send(message);

        redisTemplate.opsForValue().set(CODE_PREFIX + email, code, CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        log.info("验证码已发送至邮箱：{}，验证码：{}", email, code);
    }

    /**
     * 验证验证码
     */
    @Override
    public boolean verifyCode(String email, String code) {
        String storedCode = redisTemplate.opsForValue().get(CODE_PREFIX + email);
        if (storedCode == null) {
            return false;
        }
        boolean isValid = storedCode.equalsIgnoreCase(code);
        if (isValid) {
            redisTemplate.delete(CODE_PREFIX + email);
        }
        return isValid;
    }

    /**
     * 生成随机验证码
     */
    private String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
