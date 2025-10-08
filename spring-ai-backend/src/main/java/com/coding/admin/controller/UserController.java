package com.coding.admin.controller;

import com.coding.admin.common.Result;
import com.coding.admin.model.request.RefreshTokenRequest;
import com.coding.admin.model.request.SendCodeRequest;
import com.coding.admin.model.request.UserLoginRequest;
import com.coding.admin.model.request.UserRegisterRequest;
import com.coding.admin.model.vo.TokenVO;
import com.coding.admin.model.vo.UserLoginVO;
import com.coding.admin.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 用户控制器 - 支持双Token机制
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "发送邮箱验证码")
    @PostMapping("/send-code")
    public Result<Void> sendVerificationCode(@Valid @RequestBody SendCodeRequest request) {
        userService.sendVerificationCode(request);
        return Result.success();
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody UserRegisterRequest request) {
        Long userId = userService.register(request);
        return Result.success(userId);
    }

    @Operation(summary = "用户登录", description = "返回Access Token（2小时）和Refresh Token（7天）")
    @PostMapping("/login")
    public Result<UserLoginVO> login(@Valid @RequestBody UserLoginRequest request) {
        UserLoginVO loginVO = userService.login(request);
        return Result.success(loginVO);
    }

    @Operation(summary = "刷新Token", description = "使用Refresh Token获取新的Access Token")
    @PostMapping("/refresh-token")
    public Result<TokenVO> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenVO tokenVO = userService.refreshToken(request);
        return Result.success(tokenVO);
    }

    @Operation(summary = "用户登出", description = "删除Redis中的Refresh Token，使其失效")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody RefreshTokenRequest request) {
        userService.logout(request.getRefreshToken());
        return Result.success();
    }
}
