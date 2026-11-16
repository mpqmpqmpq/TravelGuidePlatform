package com.mpq.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.common.Result;
import com.mpq.pojo.Like;
import com.mpq.pojo.vo.LikeWithArticleVo;

public interface LikeService extends IService<Like> {

    Result selectLikeWithArticle(Page<LikeWithArticleVo> page, Integer userId);

    boolean likeArticle(Integer articleId, Integer userId);

    boolean cancelLikeArticle(Integer articleId, Integer userId);

    boolean likeComment(Integer commentId, Integer userId);

    boolean cancelLikeComment(Integer commentId, Integer userId);

    Result getArticleLikeStatus(Integer articleId, Integer userId);

    void deleteByArticleId(Integer articleId);
}
