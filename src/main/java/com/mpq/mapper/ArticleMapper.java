package com.mpq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.pojo.Article;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {


    @Insert("INSERT INTO articles (user_id, title, content, cover_image, created_at, updated_at, view_count, comment_count, like_count, status, category_id) " +
            "VALUES (#{userId}, #{title}, #{content}, #{coverImage}, #{createdAt}, #{updatedAt}, #{viewCount}, #{commentCount}, #{likeCount}, #{status}, #{categoryId})")
    void publishArticles(Article article);

    int updateById(Article article);

    int updateViewCountBatch(
            @Param("articleId") Integer articleId,
            @Param("increment") Integer increment
    );

    @Select("SELECT category_id FROM articles WHERE article_id = #{articleId}")
    Integer selectCategoryByArticleId(Integer articleId);


    List<Integer> selectBrowsedCategories(Integer userId, int day);

    List<Article> recallArticles(int statusPublish, LocalDateTime startTime, List<Integer> preferCategories, List<String> preferTags, int recallLimit, Integer userId);

    Page<Article> searchArticles(Page<Article> page, String keyword, String sort);
}
