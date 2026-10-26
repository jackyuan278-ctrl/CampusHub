package com.campushub.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.user.domain.dto.ChangePasswordDTO;
import com.campushub.user.domain.dto.LoginFormDTO;
import com.campushub.user.domain.dto.RegisterFormDTO;
import com.campushub.user.domain.dto.UpdateUserDTO;
import com.campushub.user.domain.po.User;
import com.campushub.user.domain.vo.UserLoginVO;
import com.campushub.user.domain.vo.UserVO;

public interface IUserService extends IService<User> {

    void register(RegisterFormDTO registerFormDTO);

    UserLoginVO login(LoginFormDTO loginFormDTO);

    void changePassword(ChangePasswordDTO changePasswordDTO);

    UserVO queryMe();

    void updateMe(UpdateUserDTO updateUserDTO);

    /** 支付服务扣减余额；bizOrderNo 是幂等键，同一订单只能扣款一次。 */
    void deductBalance(Long userId, Long bizOrderNo, Integer amount);
}




