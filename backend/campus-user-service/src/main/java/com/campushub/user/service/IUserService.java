package com.campushub.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.user.domain.dto.ChangePasswordDTO;
import com.campushub.user.domain.dto.LoginFormDTO;
import com.campushub.user.domain.dto.RegisterFormDTO;
import com.campushub.user.domain.dto.UpdateUserDTO;
import com.campushub.user.domain.po.User;
import com.campushub.user.domain.vo.UserLoginVO;
import com.campushub.api.dto.UserBriefDTO;
import com.campushub.user.domain.vo.UserVO;

import java.util.List;

public interface IUserService extends IService<User> {

    void register(RegisterFormDTO registerFormDTO);

    UserLoginVO login(LoginFormDTO loginFormDTO);

    void changePassword(ChangePasswordDTO changePasswordDTO);

    UserVO queryMe();

    void updateMe(UpdateUserDTO updateUserDTO);

    /** 只更新头像地址：上传成功后由 controller 调用，避免和资料表单的校验互相牵连。 */
    void updateAvatar(String avatarUrl);

    /** 支付服务扣减余额；bizOrderNo 是幂等键，同一订单只能扣款一次。 */
    void deductBalance(Long userId, Long bizOrderNo, Integer amount);

    /** 余额退款：支付成功但订单已被超时关单时回补，幂等由支付单状态流转保证 */
    void refundBalance(Long userId, Long payOrderId, Integer amount);

    /**
     * 批量查询用户展示信息，供其他服务拼列表时补昵称/头像。
     * 入参会去重、过滤 null 并截断到上限；用户不存在不会报错，只是结果里少几个人。
     */
    List<UserBriefDTO> queryBriefByIds(List<Long> ids);
}




