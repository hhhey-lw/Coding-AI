package com.coding.core.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coding.core.mapper.UserMapper;
import com.coding.core.model.request.RefreshTokenRequest;
import com.coding.core.model.request.SendCodeRequest;
import com.coding.core.model.request.UserLoginRequest;
import com.coding.core.model.request.UserRegisterRequest;
import com.coding.core.model.entity.UserDO;
import com.coding.core.model.vo.UserTokenVO;
import com.coding.core.model.vo.UserLoginVO;
import com.coding.core.service.EmailService;
import com.coding.core.service.CacheService;
import com.coding.core.service.UserService;
import com.coding.core.utils.JwtUtil;
import com.coding.workflow.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现 - 支持双Token机制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final EmailService emailService;
    private final CacheService cacheService;
    private final JwtUtil jwtUtil;

    private static final String VERIFICATION_CODE_PREFIX = "verification:code:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";
    private static final long CODE_EXPIRATION = 5; // 验证码过期时间（分钟）
    private static final String SALT = "longcoding.top"; // 密码加盐

    @Override
    public void sendVerificationCode(SendCodeRequest request) {
        String email = request.getEmail();

        // 生成6位随机验证码
        String code = RandomUtil.randomNumbers(6);

        // 保存验证码到Redis，5分钟过期
        String redisKey = VERIFICATION_CODE_PREFIX + email;
        cacheService.setRemote(redisKey, code, CODE_EXPIRATION, TimeUnit.MINUTES);

        // 发送邮件
        emailService.sendVerificationCode(email, code);

        log.info("验证码已发送到邮箱：{}，验证码：{}", email, code);
    }

    @Override
    public Long register(UserRegisterRequest request) {
        String email = request.getEmail();
        String password = request.getPassword();
        String code = request.getCode();

        // 1. 验证验证码
        String redisKey = VERIFICATION_CODE_PREFIX + email;
        String savedCode = cacheService.getRemote(redisKey);

        if (savedCode == null) {
            throw new BizException("验证码已过期");
        }

        if (!savedCode.equals(code)) {
            throw new BizException("验证码错误");
        }

        // 2. 检查邮箱是否已注册
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getUserAccount, email);
        UserDO existUser = userMapper.selectOne(wrapper);

        if (existUser != null) {
            throw new BizException("该邮箱已注册");
        }

        // 3. 加密密码
        String encryptedPassword = encryptPassword(password);

        // 4. 创建用户
        UserDO user = new UserDO();
        user.setUserAccount(email);
        user.setUserPassword(encryptedPassword);
        user.setUserName(request.getUserName() != null ? request.getUserName() : "用户" + RandomUtil.randomString(6));
        user.setUserRole("user");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);

        // 5. 删除Redis中的验证码
        cacheService.deleteRemote(redisKey);

        log.info("用户注册成功，邮箱：{}，用户ID：{}", email, user.getId());

        return user.getId();
    }

    @Override
    public UserLoginVO login(UserLoginRequest request) {
        String email = request.getEmail();
        String password = request.getPassword();

        // 1. 查询用户
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getUserAccount, email);
        UserDO user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BizException("用户不存在");
        }

        // 2. 验证密码
        String encryptedPassword = encryptPassword(password);
        if (!encryptedPassword.equals(user.getUserPassword())) {
            throw new BizException("密码错误");
        }

        // 3. 生成双Token
        String accessToken = jwtUtil.generateAccessToken(user.getId(), email);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), email);

        // 4. 将Refresh Token存储到Redis，过期时间与Token一致
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + user.getId();
        cacheService.setRemote(refreshTokenKey, refreshToken,
            jwtUtil.getRefreshTokenExpirationSeconds(), TimeUnit.SECONDS);

        log.info("用户登录成功，邮箱：{}，用户ID：{}", email, user.getId());

        // 5. 构建返回结果
        return UserLoginVO.builder()
                .userId(user.getId())
                .email(user.getUserAccount())
                .userName(user.getUserName())
                .userAvatar(user.getUserAvatar())
                .userRole(user.getUserRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessTokenExpirationSeconds())
                .tokenType("Bearer")
                .build();
    }

    @Override
    public UserTokenVO refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. 验证Refresh Token的有效性
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BizException("刷新令牌无效或已过期");
        }

        // 2. 从Token中获取用户信息
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String email = jwtUtil.getEmailFromToken(refreshToken);

        if (userId == null || email == null) {
            throw new BizException("刷新令牌信息不完整");
        }

        // 3. 验证Redis中的Refresh Token是否匹配
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + userId;
        String savedRefreshToken = cacheService.getRemote(refreshTokenKey);

        if (savedRefreshToken == null) {
            throw new BizException("刷新令牌已失效，请重新登录");
        }

        if (!savedRefreshToken.equals(refreshToken)) {
            throw new BizException("刷新令牌不匹配，请重新登录");
        }

        // 4. 生成新的Access Token
        String newAccessToken = jwtUtil.generateAccessToken(userId, email);

        log.info("Token刷新成功，用户ID：{}", userId);

        // 5. 返回新的Access Token（Refresh Token保持不变）
        return UserTokenVO.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getAccessTokenExpirationSeconds())
                .tokenType("Bearer")
                .build();
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null) {
            return;
        }

        // 1. 从Token中获取用户ID
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);

        if (userId == null) {
            return;
        }

        // 2. 删除Redis中的Refresh Token
        String refreshTokenKey = REFRESH_TOKEN_PREFIX + userId;
        cacheService.deleteRemote(refreshTokenKey);

        log.info("用户登出成功，用户ID：{}", userId);
    }

    @Override
    public Long getUserIdByToken(String token) {
        if (token == null || !jwtUtil.validateAccessToken(token)) {
            return null;
        }
        return jwtUtil.getUserIdFromToken(token);
    }

    @Override
    public void updateAvatar(Long userId, String avatarUrl) {
        UserDO user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setUserAvatar(avatarUrl);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        log.info("用户头像更新成功，用户ID：{}，头像URL：{}", userId, avatarUrl);
    }

    /**
     * 加密密码
     */
    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex((SALT + password).getBytes(StandardCharsets.UTF_8));
    }
}
