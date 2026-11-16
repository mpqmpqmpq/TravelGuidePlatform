package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.common.Result;
import com.mpq.mapper.TagsMapper;
import com.mpq.pojo.Tags;
import com.mpq.service.ArticleTagsService;
import com.mpq.service.TagsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TagsServiceImpl extends ServiceImpl<TagsMapper, Tags> implements TagsService {
    private final TagsMapper tagsMapper;
    private final ArticleTagsService articleTagsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> saveWithArticle(Integer articleId, List<String> tagNames) {
        List<Tags> tags = findOrCreateTags(tagNames);
        List<Integer> tagIds = tags.stream().map(Tags::getTagId).toList();
        articleTagsService.saveArticleTags(articleId, tagIds);
        return Result.success("标签保存成功");

    }

    @Override
    public List<Tags> findOrCreateTags(List<String> tagNames) {
        List<Tags> tags = new ArrayList<>();
        for (String tagName : tagNames) {
            String tagSlug = tagName.toLowerCase().replaceAll("\\s+", "-");
            Tags tag = findOrCreateTag(tagName, tagSlug);
            tags.add(tag);
        }
        return tags;
    }

    private Tags findOrCreateTag(String tagName, String tagSlug) {
        LambdaQueryWrapper<Tags> query = new LambdaQueryWrapper<>();
        query.eq(Tags::getTagName, tagName);
        Tags existingTag = tagsMapper.selectOne(query);
        if (existingTag != null) {
            return existingTag;
        }
        Tags newTag = new Tags();
        newTag.setTagName(tagName);
        newTag.setTagSlug(tagSlug);
        tagsMapper.insert(newTag);
        return newTag;
    }

    @Override
    public Page<Tags> getTags(Page<Tags> page) {
        return this.page(page);
    }

    @Override
    @Transactional
    public Result deleteTag(Integer tagId) {
        boolean deletedArticleTags = articleTagsService.deleteByTags(tagId);
        int tag = tagsMapper.deleteById(tagId);
        if (tag > 0 && deletedArticleTags) {
            return Result.success("标签删除成功");
        }
        return Result.failure(500, "标签删除失败");
    }

    @Override
    public Result deleteByIds(List<Integer> tagIds) {

        articleTagsService.deleteByTagIds(tagIds);
        int delete = tagsMapper.deleteBatchIds(tagIds);
        if (delete > 0) {
            return Result.success("标签删除成功");
        }
        return Result.failure(500, "标签删除失败");
    }


}