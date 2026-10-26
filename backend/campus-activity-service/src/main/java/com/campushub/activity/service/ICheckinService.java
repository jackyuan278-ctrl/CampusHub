package com.campushub.activity.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.activity.domain.po.Checkin;
import com.campushub.activity.domain.vo.CheckinVO;

/**
 * 签到服务（Redis Bitmap 深挖区）。
 *
 * - checkin:sign:{yyyy-MM}  Bitmap：offset = 当月第几天 - 1，SETBIT 记签到位
 * - 连续天数：从昨天往前 GETBIT 回数 + 今天 1 天
 * - tb_checkin 唯一键 (user_id, checkin_date) 兜底并发重复签到；Redis 是实时事实，DB 是持久化记录
 */
public interface ICheckinService extends IService<Checkin> {

    /**
     * 签到：今天已签抛"今日已签到"；SETBIT + 插入 tb_checkin；
     * 返回 signedToday=true、最新连续天数、本月已签日期列表
     */
    CheckinVO checkin(Long userId);

    /**
     * 查询我的签到状态：不签到，只读 GETBIT 遍历本月 + 回数连续天数
     */
    CheckinVO queryMine(Long userId);
}





