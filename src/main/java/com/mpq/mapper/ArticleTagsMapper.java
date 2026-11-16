package com.mpq.mapper;

import com.mpq.pojo.ArticleTags;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 文章标签关联表 Mapper 接口
 * </p>
 *
 * @author mpq
 * @since 2026-03-30
 */
@Mapper
public interface ArticleTagsMapper extends BaseMapper<ArticleTags> {

    List<String> selectTagNamesByArticleId(Integer articleId);

    @Delete("DELETE FROM article_tags WHERE tag_id = #{tagId}")
    boolean deleteByTagsId(Integer tagId);

    void deleteByTagIds(@Param("tagIds") List<Integer> tagIds);
}
