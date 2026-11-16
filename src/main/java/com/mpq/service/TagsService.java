package com.mpq.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.common.Result;
import com.mpq.pojo.Tags;
import java.util.List;
public interface TagsService  extends IService<Tags> {
    Result<Void> saveWithArticle(Integer articleId, List<String> tagNames);
    List<Tags> findOrCreateTags(List<String> tagNames);
  
    Page<Tags> getTags(Page<Tags> page);

    Result deleteTag(Integer tagId);

    Result deleteByIds(List<Integer> tagIds);
}