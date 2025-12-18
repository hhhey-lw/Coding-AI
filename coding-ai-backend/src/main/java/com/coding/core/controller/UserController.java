package com.coding.core.controller;

import com.coding.core.common.Result;
import com.coding.core.model.request.RefreshTokenRequest;
import com.coding.core.model.request.UserLoginRequest;
import com.coding.core.model.request.UserRegisterRequest;
import com.coding.core.model.vo.UserTokenVO;
import com.coding.core.model.vo.UserLoginVO;
import com.coding.core.service.UserService;
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
    public Result<UserTokenVO> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        UserTokenVO userTokenVO = userService.refreshToken(request);
        return Result.success(userTokenVO);
    }

    @Operation(summary = "用户登出", description = "删除Redis中的Refresh Token，使其失效")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody RefreshTokenRequest request) {
        userService.logout(request.getRefreshToken());
        return Result.success();
    }

    @Operation(summary = "更新用户头像")
    @PostMapping("/update-avatar")
    public Result<Void> updateAvatar(@RequestParam Long userId, @RequestParam String avatarUrl) {
        userService.updateAvatar(userId, avatarUrl);
        return Result.success();
    }
}
