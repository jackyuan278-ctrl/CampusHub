package com.campushub.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushub.api.dto.UserBriefDTO;
import com.campushub.common.BizException;
import com.campushub.common.interceptor.UserContext;
import com.campushub.common.utils.JwtTool;
import com.campushub.user.domain.dto.ChangePasswordDTO;
import com.campushub.user.domain.dto.LoginFormDTO;
import com.campushub.user.domain.dto.RegisterFormDTO;
import com.campushub.user.domain.dto.UpdateUserDTO;
import com.campushub.user.domain.po.User;
import com.campushub.user.domain.po.BalanceDeduction;
import com.campushub.user.domain.vo.UserLoginVO;
import com.campushub.user.domain.vo.UserVO;
import com.campushub.user.mapper.UserMapper;
import com.campushub.user.mapper.BalanceDeductionMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import com.campushub.user.service.IUserService;
import com.campushub.user.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /** 批量查询单次上限：IN 列表再长就会把一次列表页的补数据变成整表扫描 */
    private static final int MAX_BRIEF_QUERY = 100;

    private final JwtTool jwtTool;

    // 容器 Bean：passWordConfig 里 @Bean 注册，@RequiredArgsConstructor 自动注入
    private final PasswordEncoder passwordEncoder;

    private final BalanceDeductionMapper balanceDeductionMapper;

    @Override
    public void register(RegisterFormDTO registerFormDTO) {
        String username = registerFormDTO.getUsername();
        if (query().eq("username", username).exists()) {
            throw new BizException(400, "用户名已存在");
        }
        String phone = registerFormDTO.getPhone();
        if (query().eq("phone", phone).exists()) {
            throw new BizException(400, "手机号已存在");
        }
        String password = registerFormDTO.getPassword();
        String code = passwordEncoder.encode(password);
        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setPassword(code);
        save(user);
    }

    @Override
    public UserLoginVO login(LoginFormDTO loginFormDTO) {
        String username = loginFormDTO.getUsername();
        User user = lambdaQuery().eq(User::getUsername, username).one();

        // 用户名不存在和密码错误统一报错，防止攻击者枚举账号
        if (user == null || !passwordEncoder.matches(loginFormDTO.getPassword(), user.getPassword())) {
            throw new BizException(400, "用户名或密码错误");
        }

        // 校验账号状态
        if (user.getStatus() == UserStatus.DISABLED.getValue()) {
            throw new BizException(400, "账号已禁用");
        }

        // 生成JWT，组装VO
        UserLoginVO userLoginVO = new UserLoginVO();
        userLoginVO.setUsername(user.getUsername());
        userLoginVO.setUserId(user.getId());
        String token = jwtTool.createToken(user.getId());
        userLoginVO.setToken(token);
        return userLoginVO;
    }


    @Override
    public void changePassword(ChangePasswordDTO changePasswordDTO) {
        Long userId = UserContext.getUser();
        User user = getById(userId);
        if (user == null) {
            throw new BizException(400, "用户不存在");
        }
        String password = user.getPassword();
        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), password)) {
            throw new BizException(400,"原密码输入错误");
        }
        String newPassword = changePasswordDTO.getNewPassword();
        String oldPassword = changePasswordDTO.getOldPassword();
        if (newPassword.equals(oldPassword)) {
            throw new BizException(400,"前后密码一致");
        }
        update().eq("id", userId).set("password", passwordEncoder.encode(newPassword)).update();
    }

    @Override
    public UserVO queryMe() {
        Long userId = UserContext.getUser();
        User user = getById(userId);
        if (user == null) {
            throw new BizException(400, "用户不存在");
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public void updateMe(UpdateUserDTO updateUserDTO) {
        Long userId = UserContext.getUser();
        User user = getById(userId);
        if (user == null) {
            throw new BizException(400, "用户不存在");
        }
        // 手机号查重（排除自己）
        String phone = updateUserDTO.getPhone();
        if (phone != null && !phone.equals(user.getPhone())
                && query().eq("phone", phone).ne("id", userId).exists()) {
            throw new BizException(400, "手机号已被占用");
        }
        // updateById 配合 update-strategy: not_null，null 字段自动跳过
        User update = new User();
        update.setId(userId);
        update.setNickname(updateUserDTO.getNickname());
        update.setPhone(updateUserDTO.getPhone());
        update.setAvatar(updateUserDTO.getAvatar());
        updateById(update);
    }

    @Override
    public void updateAvatar(String avatarUrl) {
        Long userId = UserContext.getUser();
        if (userId == null) {
            throw new BizException(400, "请先登录");
        }
        // 只写 avatar 一列：update-strategy 是 not_null，不会把昵称、手机号覆盖成 null
        User update = new User();
        update.setId(userId);
        update.setAvatar(avatarUrl);
        updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductBalance(Long userId, Long bizOrderNo, Integer amount) {
        if (userId == null || bizOrderNo == null) {
            throw new BizException("用户或订单信息不能为空");
        }
        if (amount == null || amount <= 0) {
            throw new BizException("扣款金额必须大于0");
        }

        // 先尝试写入唯一业务订单流水。若订单号已存在，说明可能是网络超时后的安全重试。
        BalanceDeduction deduction = new BalanceDeduction();
        deduction.setBizOrderNo(bizOrderNo);
        deduction.setUserId(userId);
        deduction.setAmount(amount);
        deduction.setCreateTime(LocalDateTime.now());
        try {
            balanceDeductionMapper.insert(deduction);
        } catch (DuplicateKeyException duplicate) {
            // 唯一键在数据库层串行化并发请求；重复请求只在用户、金额一致时按成功幂等返回。
            BalanceDeduction existing = balanceDeductionMapper.selectByBizOrderNoForUpdate(bizOrderNo);
            if (existing != null && userId.equals(existing.getUserId()) && amount.equals(existing.getAmount())) {
                return;
            }
            throw new BizException("订单扣款幂等信息不匹配");
        }

        // 流水插入与余额条件扣减处于同一个本地事务：余额不足时抛错会同时回滚流水。
        boolean deducted = lambdaUpdate()
                .eq(User::getId, userId)
                .ge(User::getBalance, amount)
                .setDecrBy(User::getBalance, amount)
                .update();
        if (!deducted) {
            throw new BizException("用户不存在或余额不足");
        }
    }

    @Override
    public List<UserBriefDTO> queryBriefByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return List.of();
        }
        // 超限截断而不是抛 400：这道上限防的是调用方把整表 id 灌进来，
        // 属于保护自己的读放大，不是业务校验。为此让上游整页请求失败（列表直接 500）
        // 代价远大于少几个昵称——截断只影响展示，报错影响可用性。
        if (distinctIds.size() > MAX_BRIEF_QUERY) {
            log.warn("批量查询用户信息超过上限，已截断：请求 {} 个，只处理前 {} 个",
                    distinctIds.size(), MAX_BRIEF_QUERY);
            distinctIds = distinctIds.subList(0, MAX_BRIEF_QUERY);
        }
        return listByIds(distinctIds).stream().map(user -> {
            // 逐字段赋值而不是 BeanUtils 整体拷贝：DTO 里没有 password，
            // 从结构上保证密码哈希永远不会被别的服务拿到
            UserBriefDTO brief = new UserBriefDTO();
            brief.setId(user.getId());
            brief.setUsername(user.getUsername());
            brief.setNickname(user.getNickname());
            brief.setAvatarUrl(user.getAvatar());
            return brief;
        }).toList();
    }
}




