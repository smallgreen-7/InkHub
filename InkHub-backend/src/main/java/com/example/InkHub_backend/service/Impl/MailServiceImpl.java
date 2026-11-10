package com.example.InkHub_backend.service.Impl;

import com.example.InkHub_backend.common.BusinessException;
import com.example.InkHub_backend.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 邮件发送实现。SMTP 账号密码全部走环境变量，不落库不进代码。
 * 未配置 spring.mail.username 时，按 inkhub.mail.dev-log-code 决定是否降级为「日志输出验证码」，
 * 方便本地在没邮箱的情况下把找回密码流程跑通。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Value("${inkhub.mail.dev-log-code:true}")
    private boolean devLogCode;

    @Override
    public void sendResetCode(String to, String code) {
        // 1. 没配 SMTP 账号：开发模式打日志，生产模式直接报错
        if (from == null || from.isBlank()) {
            if (devLogCode) {
                log.warn("【开发模式·未配置 SMTP】验证码未真实发送 → email={}, code={}", to, code);
                return;
            }
            throw new BusinessException(500, "邮件服务未配置，请联系管理员");
        }

        // 2. 正常发信
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject("【InkHub】找回密码验证码");
            helper.setText(buildHtml(code), true);
            mailSender.send(message);
            log.info("找回密码验证码已发送 → {}", to);
        } catch (MessagingException | MailException e) {
            log.error("验证码邮件发送失败: {}", e.getMessage());
            throw new BusinessException(500, "验证码发送失败，请稍后重试");
        }
    }

    private String buildHtml(String code) {
        return "<div style=\"font-family:-apple-system,'PingFang SC',Arial,sans-serif;color:#0f1e2b\">"
                + "<h3 style=\"margin:0 0 12px\">InkHub 找回密码</h3>"
                + "<p>你的验证码是：</p>"
                + "<p style=\"font-size:28px;font-weight:700;letter-spacing:6px;color:#1d9fc0\">" + code + "</p>"
                + "<p style=\"color:#6b8194;font-size:13px\">验证码 5 分钟内有效，请勿转发给他人。如非本人操作请忽略本邮件。</p>"
                + "</div>";
    }
}
