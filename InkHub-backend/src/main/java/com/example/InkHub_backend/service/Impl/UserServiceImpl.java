package com.example.InkHub_backend.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.InkHub_backend.common.BusinessException;
import com.example.InkHub_backend.dto.ChangePasswordDTO;
import com.example.InkHub_backend.dto.ChangeUsernameDTO;
import com.example.InkHub_backend.dto.DeactivateDTO;
import com.example.InkHub_backend.dto.LoginDTO;
import com.example.InkHub_backend.dto.NotifyPrefDTO;
import com.example.InkHub_backend.dto.RegisterDTO;
import com.example.InkHub_backend.dto.ResetCodeDTO;
import com.example.InkHub_backend.dto.ResetPasswordDTO;
import com.example.InkHub_backend.dto.UpdateProfileDTO;
import com.example.InkHub_backend.entity.User;
import com.example.InkHub_backend.mapper.UserMapper;
import com.example.InkHub_backend.security.JwtUtil;
import com.example.InkHub_backend.service.MailService;
import com.example.InkHub_backend.service.UserService;
import com.example.InkHub_backend.utils.RedisKeys;
import com.example.InkHub_backend.vo.LoginVO;
import com.example.InkHub_backend.vo.NotifyPrefVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final SecureRandom RANDOM = new SecureRandom();
    /** 验证码有效期 */
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    /** 同一邮箱发送间隔 */
    private static final Duration SEND_INTERVAL = Duration.ofSeconds(60);
    /** 普通用户默认状态 */
    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_DISABLED = 1;

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final MailService mailService;
    private final StringRedisTemplate redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ==================== 注册 / 登录 / 我的信息 ====================

    @Override
    public void register(RegisterDTO dto) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setRole(1);            // 默认普通用户
        user.setStatus(STATUS_NORMAL);
        user.setNotifyComment(1);
        user.setNotifyLike(1);
        user.setNotifyFavorite(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == STATUS_DISABLED) {
            throw new BusinessException("账号已被禁用");
        }

        String token = jwtUtil.createToken(user.getId(), user.getRole());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        return fillVo(vo, user);
    }

    @Override
    public LoginVO me(Long userId) {
        return fillVo(new LoginVO(), requireUser(userId));
    }

    // ==================== 资料 / 密码 / 用户名 / 注销 ====================

    @Override
    public void updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = requireUser(userId);

        if (dto.getNickname() != null) {
            String nickname = dto.getNickname().trim();
            if (nickname.isEmpty()) {
                throw new BusinessException(400, "昵称不能为空");
            }
            user.setNickname(nickname);
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar().trim());
        }
        // 邮箱：空串表示解绑（updateById 不更新 null 字段，所以解绑要单独用 UpdateWrapper）
        if (dto.getEmail() != null) {
            String email = dto.getEmail().trim();
            if (email.isEmpty()) {
                userMapper.update(null, new LambdaUpdateWrapper<User>()
                        .eq(User::getId, userId)
                        .set(User::getEmail, null)
                        .set(User::getUpdatedAt, LocalDateTime.now()));
            } else {
                Long used = userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, email)
                        .ne(User::getId, userId));
                if (used > 0) {
                    throw new BusinessException(400, "该邮箱已被其他账号使用");
                }
                user.setEmail(email);
            }
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码不正确");
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BusinessException(400, "新密码不能与原密码相同");
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getPassword, passwordEncoder.encode(dto.getNewPassword()))
                .set(User::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public void changeUsername(Long userId, ChangeUsernameDTO dto) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "密码不正确");
        }
        String username = dto.getUsername().trim();
        if (username.equalsIgnoreCase(user.getUsername())) {
            throw new BusinessException(400, "新用户名与当前用户名相同");
        }
        Long used = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .ne(User::getId, userId));
        if (used > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getUsername, username)
                .set(User::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public void deactivate(Long userId, DeactivateDTO dto) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "密码不正确");
        }
        // 兜底：不能把最后一个可用管理员注销掉
        if (user.getRole() != null && user.getRole() == 2) {
            Long admins = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, 2)
                    .eq(User::getStatus, STATUS_NORMAL));
            if (admins <= 1) {
                throw new BusinessException(400, "这是最后一个可用管理员，不能注销");
            }
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, STATUS_DISABLED)
                .set(User::getUpdatedAt, LocalDateTime.now()));
        log.info("用户 {} 已注销（软删除）", userId);
    }

    // ==================== 通知偏好 ====================

    @Override
    public NotifyPrefVO notifyPref(Long userId) {
        User user = requireUser(userId);
        NotifyPrefVO vo = new NotifyPrefVO();
        vo.setNotifyComment(defaultOn(user.getNotifyComment()));
        vo.setNotifyLike(defaultOn(user.getNotifyLike()));
        vo.setNotifyFavorite(defaultOn(user.getNotifyFavorite()));
        return vo;
    }

    @Override
    public void updateNotifyPref(Long userId, NotifyPrefDTO dto) {
        requireUser(userId);
        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getUpdatedAt, LocalDateTime.now());
        if (dto.getNotifyComment() != null) {
            wrapper.set(User::getNotifyComment, dto.getNotifyComment());
        }
        if (dto.getNotifyLike() != null) {
            wrapper.set(User::getNotifyLike, dto.getNotifyLike());
        }
        if (dto.getNotifyFavorite() != null) {
            wrapper.set(User::getNotifyFavorite, dto.getNotifyFavorite());
        }
        userMapper.update(null, wrapper);
    }

    // ==================== 忘记密码 ====================

    @Override
    public void sendResetCode(ResetCodeDTO dto) {
        String email = normalizeEmail(dto.getEmail());

        // 1. 60 秒内只允许发一次，防止短信/邮件轰炸
        if (Boolean.TRUE.equals(redisTemplate.hasKey(RedisKeys.resetLimit(email)))) {
            throw new BusinessException(429, "验证码发送太频繁，请 60 秒后再试");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .last("limit 1"));

        // 2. 邮箱没注册 / 账号已禁用：静默返回成功，避免被用来探测哪些邮箱注册过
        if (user == null || (user.getStatus() != null && user.getStatus() == STATUS_DISABLED)) {
            log.info("找回密码请求：邮箱 {} 未匹配到可用账号，静默忽略", email);
            return;
        }

        // 3. 生成 6 位验证码，存 Redis（5 分钟）
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(RedisKeys.resetCode(email), code, CODE_TTL);
        redisTemplate.opsForValue().set(RedisKeys.resetLimit(email), "1", SEND_INTERVAL);

        mailService.sendResetCode(email, code);
    }

    @Override
    public void resetPassword(ResetPasswordDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        String saved = redisTemplate.opsForValue().get(RedisKeys.resetCode(email));
        if (saved == null) {
            throw new BusinessException(400, "验证码已过期，请重新获取");
        }
        if (!saved.equals(dto.getCode().trim())) {
            throw new BusinessException(400, "验证码不正确");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .last("limit 1"));
        if (user == null) {
            throw new BusinessException(400, "该邮箱未绑定任何账号");
        }
        if (user.getStatus() != null && user.getStatus() == STATUS_DISABLED) {
            throw new BusinessException(400, "账号已被禁用，无法重置密码");
        }

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getPassword, passwordEncoder.encode(dto.getNewPassword()))
                .set(User::getUpdatedAt, LocalDateTime.now()));

        // 验证码一次性使用，成功即删
        redisTemplate.delete(RedisKeys.resetCode(email));
        log.info("用户 {} 通过邮箱验证码重置了密码", user.getId());
    }

    // ==================== 私有工具方法 ====================

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || (user.getStatus() != null && user.getStatus() == STATUS_DISABLED)) {
            throw new BusinessException(401, "账号不存在或已注销");
        }
        return user;
    }

    private LoginVO fillVo(LoginVO vo, User user) {
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setNotifyComment(defaultOn(user.getNotifyComment()));
        vo.setNotifyLike(defaultOn(user.getNotifyLike()));
        vo.setNotifyFavorite(defaultOn(user.getNotifyFavorite()));
        return vo;
    }

    private Integer defaultOn(Integer value) {
        return value == null ? 1 : value;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
