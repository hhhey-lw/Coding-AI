package com.coding.core.service;

import com.coding.core.model.request.RefreshTokenRequest;
import com.coding.core.model.request.SendCodeRequest;
import com.coding.core.model.request.UserLoginRequest;
import com.coding.core.model.request.UserRegisterRequest;
import com.coding.core.model.vo.UserTokenVO;
import com.coding.core.model.vo.UserLoginVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 发送邮箱验证码
     */
    void sendVerificationCode(SendCodeRequest request);

    /**
     * 用户注册
     */
    Long register(UserRegisterRequest request);

    /**
     * 用户登录
     */
    UserLoginVO login(UserLoginRequest request);

    /**
     * 刷新Token
     */
    UserTokenVO refreshToken(RefreshTokenRequest request);

    /**
     * 用户登出
     */
    void logout(String refreshToken);

    /**
     * 根据Token获取用户ID
     */
    Long getUserIdByToken(String token);
}
