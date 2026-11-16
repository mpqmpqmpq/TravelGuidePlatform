package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.common.Result;
import com.mpq.mapper.ArticleTagsMapper;
import com.mpq.pojo.ArticleTags;
import com.mpq.service.ArticleTagsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleTagsServiceImpl extends ServiceImpl<ArticleTagsMapper, ArticleTags> implements ArticleTagsService {
    private final ArticleTagsMapper articleTagsMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> saveArticleTags(Integer articleId, List<Integer> tagIds) {
        deleteByArticleIdData(articleId);
        if (tagIds == null || tagIds.isEmpty()) {
            return Result.success("文章标签保存成功");
        }
        List<ArticleTags> articleTagsList = new ArrayList<>();
        for (Integer tagId : tagIds) {
            ArticleTags articleTags = new ArticleTags();
            articleTags.setArticleId(articleId);
            articleTags.setTagId(tagId);
            articleTagsList.add(articleTags);
        }
        for (ArticleTags articleTags : articleTagsList) {
            articleTagsMapper.insert(articleTags);
        }
        return Result.success("文章标签保存成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteByArticleId(Integer articleId) {
        deleteByArticleIdData(articleId);
        return Result.success("文章标签删除成功");
    }

    private void deleteByArticleIdData(Integer articleId) {
        LambdaQueryWrapper<ArticleTags> query = new LambdaQueryWrapper<>();
        query.eq(ArticleTags::getArticleId, articleId);
        articleTagsMapper.delete(query);
    }

    @Override
    public List<ArticleTags> listByArticleId(Integer articleId) {
        LambdaQueryWrapper<ArticleTags> query = new LambdaQueryWrapper<>();
        query.eq(ArticleTags::getArticleId, articleId);
        return articleTagsMapper.selectList(query);
    }

    @Override
    public Page<ArticleTags> getTagsList(Page<ArticleTags> page) {
        return this.page(page);
    }

    @Override
    public boolean deleteByTags(Integer tagId) {
        return articleTagsMapper.deleteByTagsId(tagId);
    }

    @Override
    public void deleteByTagIds(List<Integer> tagIds) {
        articleTagsMapper.deleteByTagIds(tagIds);
    }
}