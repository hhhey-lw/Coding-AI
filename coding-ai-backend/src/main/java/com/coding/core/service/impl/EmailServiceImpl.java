package com.coding.core.service.impl;

import com.coding.core.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendVerificationCode(String toEmail, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("验证码");
            message.setText("您的验证码是：" + code + "，有效期5分钟，请勿泄露给他人。");

            mailSender.send(message);
            log.info("验证码邮件发送成功，收件人：{}", toEmail);
        } catch (Exception e) {
            log.error("验证码邮件发送失败，收件人：{}，错误：{}", toEmail, e.getMessage());
            throw new RuntimeException("验证码发送失败");
        }
    }
}
