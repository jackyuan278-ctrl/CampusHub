package com.campushub.user.controller;

import com.campushub.common.Result;
import com.campushub.user.domain.dto.ChangePasswordDTO;
import com.campushub.user.domain.dto.LoginFormDTO;
import com.campushub.user.domain.dto.RegisterFormDTO;
import com.campushub.user.domain.dto.UpdateUserDTO;
import com.campushub.user.domain.vo.UserLoginVO;
import com.campushub.user.domain.vo.UserVO;
import com.campushub.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    /** 免鉴权（网关白名单） */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Validated RegisterFormDTO registerFormDTO) {
        userService.register(registerFormDTO);
        return Result.success();
    }

    /** 免鉴权（网关白名单） */
    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody @Validated LoginFormDTO loginFormDTO) {
        return Result.success(userService.login(loginFormDTO));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody @Validated ChangePasswordDTO changePasswordDTO) {
        userService.changePassword(changePasswordDTO);
        return Result.success();
    }

    @GetMapping("/me")
    public Result<UserVO> queryMe() {
        return Result.success(userService.queryMe());
    }

    @PutMapping("/me")
    public Result<Void> updateMe(@RequestBody @Validated UpdateUserDTO updateUserDTO) {
        userService.updateMe(updateUserDTO);
        return Result.success();
    }

    /** 内部接口：支付服务扣减余额（Feign 直连 user-service，不经网关） */
    @PutMapping("/{id}/balance/deduct")
    public Result<Void> deductBalance(@PathVariable("id") Long userId,
                                      @RequestParam("orderId") Long orderId,
                                      @RequestParam("amount") Integer amount) {
        userService.deductBalance(userId, orderId, amount);
        return Result.success();
    }
}




