package com.campushub.community.client;

import com.campushub.api.client.UserClient;
import com.campushub.api.dto.UserBriefDTO;
import com.campushub.common.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 作者信息的唯一查询入口：把 UserClient 的 Result 拆包和失败降级收在一处。
 * <p>
 * 为什么值得单独抽一层：原来本地 selectBatchIds 是同步 JDBC，要么成功要么抛异常；
 * 换成 Feign 后多了网络这一跳，超时、连接被拒、用户服务重启都会变成异常。
 * 四个读接口（问题列表/问题详情/回答/评论/关注）都需要同一个取舍，分散写四份必然写歪，
 * 所以把"降级成空 Map"的决策固化在这里，各调用方只管拿 Map、拿不到就走兜底文案。
 * <p>
 * 取舍：作者信息缺失只影响昵称展示，让整个列表页 500 才是真事故，
 * 因此这里吞掉异常只记 warn，绝不向上抛。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserLookup {

    /** 与 user-service 批量接口的上限对齐，避免发出去一个注定会被截断的请求 */
    private static final int MAX_BATCH = 100;

    private final UserClient userClient;

    /**
     * 按 id 批量取作者信息。返回的 Map 保证非 null；任何失败都返回空 Map。
     */
    public Map<Long, UserBriefDTO> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        // 同一页里同一个作者会出现多次，去重既省一次传输，也让下面 toMap 不会撞键
        List<Long> distinctIds = ids.stream().filter(Objects::nonNull).distinct().limit(MAX_BATCH).toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        try {
            Result<List<UserBriefDTO>> result = userClient.queryUsersByIds(distinctIds);
            // code != 200 说明用户服务主动拒绝了（比如内部密钥没配对），照样按"查不到"处理：
            // 展示层降级比把内部鉴权错误抛给前端更合适，真正的原因已经落在 warn 日志里
            if (result == null || result.getCode() == null || result.getCode() != 200 || result.getData() == null) {
                log.warn("查询作者信息失败，昵称将使用兜底文案：ids={}, msg={}",
                        distinctIds, result == null ? "无响应" : result.getMsg());
                return Map.of();
            }
            return result.getData().stream()
                    .filter(Objects::nonNull)
                    .filter(user -> user.getId() != null)
                    .collect(Collectors.toMap(UserBriefDTO::getId, Function.identity(), (first, second) -> first));
        } catch (Exception e) {
            // 超时、连接拒绝、下游 5xx、返回值反序列化失败都归到这里
            log.warn("查询作者信息异常，昵称将使用兜底文案：ids={}", distinctIds, e);
            return Map.of();
        }
    }
}
