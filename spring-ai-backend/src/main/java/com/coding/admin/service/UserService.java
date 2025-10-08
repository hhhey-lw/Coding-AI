package com.coding.admin.service;

import com.coding.admin.model.request.RefreshTokenRequest;
import com.coding.admin.model.request.SendCodeRequest;
import com.coding.admin.model.request.UserLoginRequest;
import com.coding.admin.model.request.UserRegisterRequest;
import com.coding.admin.model.vo.TokenVO;
import com.coding.admin.model.vo.UserLoginVO;

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
    TokenVO refreshToken(RefreshTokenRequest request);

    /**
     * 用户登出
     */
    void logout(String refreshToken);

    /**
     * 根据Token获取用户ID
     */
    Long getUserIdByToken(String token);
}
