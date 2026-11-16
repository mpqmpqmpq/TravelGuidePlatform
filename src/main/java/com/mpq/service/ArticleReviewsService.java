package com.mpq.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.common.Result;
import com.mpq.pojo.ArticleReview;
import com.mpq.pojo.vo.ArticleReviewWithUserVO;

public interface ArticleReviewsService extends IService<ArticleReview> {
    Result getArticleReviews(Page<ArticleReview> page);
    
    Result ReviewArticles(ArticleReview articleReview);
    
    /**
     * 分页查询审批表与用户表连表数据
     *
     * @param page   分页参数
     * @param status
     * @return 连表查询结果
     */
    Result getArticleReviewsWithUser(Page<ArticleReviewWithUserVO> page, Integer status);

    ArticleReview getByArticleId(Integer articleId);
}
