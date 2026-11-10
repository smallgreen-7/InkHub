package com.example.InkHub_backend.service;

import com.example.InkHub_backend.dto.ChangePasswordDTO;
import com.example.InkHub_backend.dto.ChangeUsernameDTO;
import com.example.InkHub_backend.dto.DeactivateDTO;
import com.example.InkHub_backend.dto.LoginDTO;
import com.example.InkHub_backend.dto.NotifyPrefDTO;
import com.example.InkHub_backend.dto.RegisterDTO;
import com.example.InkHub_backend.dto.ResetCodeDTO;
import com.example.InkHub_backend.dto.ResetPasswordDTO;
import com.example.InkHub_backend.dto.UpdateProfileDTO;
import com.example.InkHub_backend.vo.LoginVO;
import com.example.InkHub_backend.vo.NotifyPrefVO;

public interface UserService {

    void register(RegisterDTO dto);

    LoginVO login(LoginDTO dto);

    /** 当前登录用户信息（含邮箱与通知偏好） */
    LoginVO me(Long userId);

    /** 修改昵称 / 头像 / 邮箱 */
    void updateProfile(Long userId, UpdateProfileDTO dto);

    /** 修改密码：校验原密码，改完前端需要重新登录 */
    void changePassword(Long userId, ChangePasswordDTO dto);

    /** 修改用户名：校验密码 + 唯一性 */
    void changeUsername(Long userId, ChangeUsernameDTO dto);

    /** 注销账号（软删除，status=1） */
    void deactivate(Long userId, DeactivateDTO dto);

    /** 查询通知偏好 */
    NotifyPrefVO notifyPref(Long userId);

    /** 修改通知偏好 */
    void updateNotifyPref(Long userId, NotifyPrefDTO dto);

    /** 忘记密码：给注册邮箱发验证码（未绑定邮箱 / 邮箱不存在都返回成功，防账号枚举） */
    void sendResetCode(ResetCodeDTO dto);

    /** 忘记密码：校验验证码并重置密码 */
    void resetPassword(ResetPasswordDTO dto);
}
