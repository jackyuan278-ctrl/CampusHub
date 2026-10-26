package com.campushub.community.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.campushub.community.domain.po.Tag;
import com.campushub.community.domain.vo.TagVO;

import java.util.List;

/**
 * 标签服务：问题标签的查询。
 */
public interface ITagService extends IService<Tag> {

    /**
     * 全部标签：按 question_count 倒序，组装 TagVO
     */
    List<TagVO> listAll();
}





