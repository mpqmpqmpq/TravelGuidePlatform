package com.mpq.service;

public interface EmailService {
    void sendVerificationCode(String email);
    
    boolean verifyCode(String email, String code);
}
