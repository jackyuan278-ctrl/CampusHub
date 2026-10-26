package com.campushub.activity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.activity.domain.dto.SignupFormDTO;
import com.campushub.activity.domain.po.Signup;
import com.campushub.activity.domain.vo.SignupVO;

import java.util.List;

public interface ISignupService extends IService<Signup> {

    /**
     * 报名：校验活动存在且报名中 → 执行 signup.lua（0成功 1已满 2重复）
     * → 0 时写 tb_signup(status=1)；1 时抛 BizException("名额已满，可加入候补")；2 时抛 BizException("请勿重复报名")。
     * 重新报名场景（曾取消）：tb_signup 有唯一键，按"有则更新状态、无则插入"处理。
     */
    void signup(SignupFormDTO form, Long userId);

    /**
     * 加入候补：校验未报名 → 写 tb_signup(status=2)（唯一键兜底防重复）→ RPUSH signup:waitlist:{activityId}
     */
    void joinWaitlist(SignupFormDTO form, Long userId);

    /**
     * 取消报名/候补：
     * 已报名 → 执行 cancel_signup.lua（-1未报名 0无补位 >0补位用户ID）
     *   → 取消者 tb_signup 1→3；返回值 >0 时补位者 tb_signup 2→1；
     * 候补中 → LREM signup:waitlist:{activityId} 掉自己 + tb_signup 2→3。
     */
    void cancel(Long activityId, Long userId);

    /**
     * 我的报名+候补列表（含活动标题/封面/地点/时间；候补按入队顺序给 waitPosition）
     */
    List<SignupVO> queryMine(Long userId);
}





