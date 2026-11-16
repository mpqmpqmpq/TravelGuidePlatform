package com.mpq.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.ArticleTags;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 文章标签关联表 服务类
 * </p>
 *
 * @author mpq
 * @since 2026-03-30
 */
public interface ArticleTagsService extends IService<ArticleTags> {

    Result saveArticleTags(Integer articleId, List<Integer> tagIds);

    Result deleteByArticleId(Integer articleId);

    List<ArticleTags> listByArticleId(Integer articleId);

    Page<ArticleTags> getTagsList(Page<ArticleTags> page);

    boolean deleteByTags(Integer tagId);

    void deleteByTagIds(List<Integer> tagIds);
}
