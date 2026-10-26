package com.campushub.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.community.domain.po.QuestionTag;
import com.campushub.community.domain.po.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {
    @Select("select *FROM tb_question_tag where tag_id =#{tagId}")
    List<QuestionTag> selectByTagId(Long tagId);
}





