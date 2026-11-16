package com.mpq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.pojo.ArticleReview;
import com.mpq.pojo.vo.ArticleReviewWithUserVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArticleReviewMapper extends BaseMapper<ArticleReview> {

    @Insert("INSERT INTO article_reviews (article_id, status, created_at, reviewed_at, reason) " +
            "VALUES (#{articleId}, #{status}, #{createdAt}, #{reviewedAt}, #{reason})")
    int insert(ArticleReview articleReview);


    void updateByArticleId(ArticleReview articleReview);

    Page<ArticleReviewWithUserVO> selectArticleReviewsWithUser(Page<ArticleReviewWithUserVO> page, Integer status);
}

