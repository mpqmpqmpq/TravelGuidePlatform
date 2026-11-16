package com.mpq.service;

import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.pojo.Article;

import java.util.List;

public interface ArticleService extends IService<Article> {
    /**
     * 发表文章
     * @param article
     * @return
     */
    Article publishArticles(Article article) throws NoApiKeyException, InputRequiredException;

    /**
     * 获取用户文章列表
     *
     * @param page
     * @param userId
     * @param status
     * @return
     */
    Page<Article> getPageByUserId(Page<Article> page, int userId,Integer  status);

    /**
     * 获取文章列表
     * @param page
     * @return
     */
    Page<Article> getArticlePageList(Page<Article> page);

    /**
     * 获取文章列表
     * @param articlePage
     * @param categoryId
     * @return
     */
    Page<Article> getArticlePageListByCategoryId(Page<Article> articlePage, Integer categoryId);

    boolean updateArticleStatus(Article article);


    /**
     * 异步累加阅读量
     */
    void incrementViewCount(Integer articleId);

    /**
     * 批量更新阅读量
     */
    void batchUpdateViewCount(Integer articleId, Integer increment);

    /**
     * 带缓存+防击穿的文章详情查询
     */
    Article getArticleDetail(Integer articleId);

    List<Article> getHotArticles();

    long getArticleAllView();

    Page<Article> searchArticles(Page<Article> page, String keyword, String sort);

    void deleteById(Integer articleId,Integer userId);

    Page<Article> getArticle(Page<Article> articlePage);
}
