package com.mpq.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.pojo.Comment;

import java.util.List;

public interface CommentsService extends IService<Comment> {
    Page<Comment> getCommentsList(Page<Comment>  page, Integer articleId);

    List<Comment> getCommentTree(Integer articleId);

    void deleteCommentsById(Integer commentId);

    boolean addComment(Comment comment);

    Page<Comment> getList(Page<Comment> page);

    void deleteByArticleId(Integer articleId);
}
