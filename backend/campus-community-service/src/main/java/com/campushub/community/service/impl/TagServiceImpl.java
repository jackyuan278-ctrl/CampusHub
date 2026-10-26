package com.campushub.community.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushub.community.domain.po.Tag;
import com.campushub.community.mapper.TagMapper;
import com.campushub.community.service.ITagService;
import com.campushub.community.domain.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 实现留白：核心业务由你实现（ITagService 的 javadoc 即契约）。
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements ITagService {

    @Override
    public List<TagVO> listAll() {
        // TODO 由你实现：orderByDesc(Tag::getQuestionCount) 查全部 → 组装 TagVO
        List<Tag> list = lambdaQuery().orderByDesc(Tag::getQuestionCount).list();
        List<TagVO> tagVOList = new ArrayList<>();
        for (Tag tag : list) {
            tagVOList.add(BeanUtil.copyProperties(tag, TagVO.class));
        }
        return tagVOList;
    }
}





