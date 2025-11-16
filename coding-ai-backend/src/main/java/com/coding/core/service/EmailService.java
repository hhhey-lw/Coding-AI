package com.coding.core.service;

/**
 * 邮件服务接口
 */
public interface EmailService {

    /**
     * 发送验证码邮件
     */
    void sendVerificationCode(String toEmail, String code);
}
