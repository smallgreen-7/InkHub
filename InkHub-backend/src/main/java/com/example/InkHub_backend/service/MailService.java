package com.example.InkHub_backend.service;

/**
 * 邮件服务：目前只用于「忘记密码」发送验证码。
 */
public interface MailService {

    /**
     * 发送找回密码验证码。
     *
     * @param to   收件人邮箱
     * @param code 6 位数字验证码
     */
    void sendResetCode(String to, String code);
}
