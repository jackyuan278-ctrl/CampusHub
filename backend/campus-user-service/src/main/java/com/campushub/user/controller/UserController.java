package com.campushub.user.controller;

import com.campushub.api.dto.UserBriefDTO;
import com.campushub.common.Result;
import com.campushub.common.utils.OssUtil;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;
    private final OssUtil ossUtil;

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

    /** 上传头像：传 OSS 后立刻写库，返回可直接展示的新地址 */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = ossUtil.upload(file);
        userService.updateAvatar(url);
        return Result.success(url);
    }

    /** 内部接口：支付服务扣减余额（Feign 直连 user-service，不经网关） */
    @PutMapping("/{id}/balance/deduct")
    public Result<Void> deductBalance(@PathVariable("id") Long userId,
                                      @RequestParam("orderId") Long orderId,
                                      @RequestParam("amount") Integer amount) {
        userService.deductBalance(userId, orderId, amount);
        return Result.success();
    }

    /** 内部接口：支付服务回补余额（订单已关闭但钱已扣，Feign 直连） */
    @PutMapping("/{id}/balance/refund")
    public Result<Void> refundBalance(@PathVariable("id") Long userId,
                                      @RequestParam("orderId") Long payOrderId,
                                      @RequestParam("amount") Integer amount) {
        userService.refundBalance(userId, payOrderId, amount);
        return Result.success();
    }

    /**
     * 内部接口：批量查用户昵称/头像（社区服务补作者信息用，Feign 直连不经网关）。
     * 放在 /users/batch 而不是 /users/{id}：后者会被网关当成普通资料查询，公网就能枚举用户。
     */
    @PostMapping("/batch")
    public Result<List<UserBriefDTO>> queryBatch(@RequestBody List<Long> ids) {
        return Result.success(userService.queryBriefByIds(ids));
    }
}




