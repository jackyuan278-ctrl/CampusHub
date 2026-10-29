package com.campushub.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.activity.domain.po.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ActivityMapper extends BaseMapper<Activity> {

    /**
     * 占一个名额。一条 SQL 同时完成"判断还有没有"和"扣减"，是防超卖的唯一裁决点。
     * 返回 1 = 占到；返回 0 = 已满（条件不成立）。
     * UPDATE 是当前读，不受事务快照影响，并发下不会读到旧余量。
     */
    @Update("UPDATE tb_activity SET remain_quota = remain_quota - 1 " +
            "WHERE id = #{id} AND remain_quota > 0")
    int takeQuota(@Param("id") Long id);

    /**
     * 归还一个名额。上限不超过总名额，防止取消次数多于报名次数导致余量虚高。
     */
    @Update("UPDATE tb_activity SET remain_quota = remain_quota + 1 " +
            "WHERE id = #{id} AND remain_quota < quota")
    int giveBackQuota(@Param("id") Long id);

    /**
     * 读取真实余量。只用于重建 Redis 缓存，不参与任何判断。
     */
    @Select("SELECT remain_quota FROM tb_activity WHERE id = #{id}")
    Integer selectRemainQuota(@Param("id") Long id);
}





