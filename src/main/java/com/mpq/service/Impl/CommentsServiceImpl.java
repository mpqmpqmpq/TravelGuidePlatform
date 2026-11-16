package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.mapper.CommentsMapper;
import com.mpq.pojo.Article;
import com.mpq.pojo.Comment;
import com.mpq.pojo.User;
import com.mpq.service.ArticleService;
import com.mpq.service.CommentsService;
import com.mpq.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentsServiceImpl extends ServiceImpl<CommentsMapper, Comment> implements CommentsService {
    private final CommentsMapper commentsMapper;
    private final UserService userService;
    private final ArticleService articleService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Page<Comment> getCommentsList(Page<Comment> page, Integer articleId) {
        String commentsKey = "comment:list:" + articleId;
        Object commentsList = redisTemplate.opsForValue().get(commentsKey);
        if (commentsList != null) {
            return (Page<Comment>) commentsList;
        }
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getArticleId, articleId)
                .orderByAsc(Comment::getCreatedAt);
        int randomTime = 30 + new Random().nextInt(10);
        redisTemplate.opsForValue().set(commentsKey, page, randomTime, TimeUnit.MINUTES);
        return this.page(page, queryWrapper);

    }

    @Override
    public List<Comment> getCommentTree(Integer articleId) {
        String commentsKey = "comment:tree:" + articleId;
        Object redisComments = redisTemplate.opsForValue().get(commentsKey);
        if (redisComments != null) {
            return (List<Comment>) redisComments;
        }
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getArticleId, articleId)
                .orderByAsc(Comment::getPath);
        List<Comment> comments = this.list(queryWrapper);
        List<Comment> tree = buildCommentTree(comments);
        int randomTime = 30 + new Random().nextInt(10);
        redisTemplate.opsForValue().set(commentsKey, tree, randomTime, TimeUnit.MINUTES);
        return tree;
    }

    @Transactional
    @Override
    public void deleteCommentsById(Integer commentId) {
        Comment comment = commentsMapper.selectById(commentId);
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getParentId, commentId);
        List<Comment> comments = commentsMapper.selectList(queryWrapper);
        if (!comments.isEmpty()) {
            for (Comment pComment : comments) {
                commentsMapper.deleteById(pComment.getCommentId());
            }
        }
        commentsMapper.deleteById(commentId);
        if (comment != null) {
            String commentsKey = "comment:tree:" + comment.getArticleId();
            redisTemplate.delete(commentsKey);
            String commentsKeyList = "comment:list:" + comment.getArticleId() + "*";
            redisTemplate.delete(commentsKeyList);
        }
    }

    @Override
    public boolean addComment(Comment comment) {
        Article article = articleService.getById(comment.getArticleId());
        article.setCommentCount(article.getCommentCount() + 1);
        articleService.updateById(article);
        User userById = userService.getUserById(comment.getUserId());
        comment.setUserName(userById.getUserName());
        comment.setUserAvatar(userById.getAvatarUrl());
        boolean result = this.save(comment);
        if (result) {
            String commentsKey = "comment:tree:" + comment.getArticleId();
            redisTemplate.delete(commentsKey);
            String commentsKeyList = "comment:list:" + comment.getArticleId() + "*";
            redisTemplate.delete(commentsKeyList);
        }
        return result;
    }

    @Override
    public Page<Comment> getList(Page<Comment> page) {
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(Comment::getCreatedAt);
        return this.page(page, queryWrapper);
    }

    @Override
    public void deleteByArticleId(Integer articleId) {
        LambdaQueryWrapper<Comment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Comment::getArticleId, articleId);
        this.remove(queryWrapper);
    }

    private List<Comment> buildCommentTree(List<Comment> comments) {
        Map<Integer, Comment> commentMap = comments.stream()
                .collect(Collectors.toMap(Comment::getCommentId, comment -> comment));
        // 结果集：存储顶级评论
        List<Comment> rootComments = new ArrayList<>();

        for (Comment comment : comments) {
            // 修正：只将顶级评论添加到 rootComments 列表
            if (comment.getParentId() == null || comment.getParentId() == 0) {
                rootComments.add(comment);
            } else {
                // 如果是子评论，则找到其父评论并添加到父评论的 children 列表中
                Comment parentComment = commentMap.get(comment.getParentId());
                if (parentComment != null) {
                    if (parentComment.getChildren() == null) {
                        parentComment.setChildren(new ArrayList<>());
                    }
                    parentComment.getChildren().add(comment);
                }
            }
        }
        return rootComments;
    }
}
