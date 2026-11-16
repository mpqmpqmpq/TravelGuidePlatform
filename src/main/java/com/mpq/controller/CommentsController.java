package com.mpq.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.Article;
import com.mpq.pojo.Comment;
import com.mpq.service.ArticleService;
import com.mpq.service.CommentsService;
import com.mpq.utils.NotificationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comments")
@Slf4j
@RequiredArgsConstructor
public class CommentsController {

    private final CommentsService commentsService;
    private final NotificationHelper notificationHelper;
    private final ArticleService articleService;

    @GetMapping("/getCommentsList/{articleId}")
    public Result getCommentsList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size,
            @PathVariable Integer articleId) {
        Page<Comment> page = new Page<>(pageNum, size);
        Page<Comment> commentsList = commentsService.getCommentsList(page, articleId);
        Map<String, Object> commentsMap = new HashMap<>();
        commentsMap.put("commentsList", commentsList);
        commentsMap.put("total", commentsList.getTotal());
        commentsMap.put("pages", commentsList.getPages());
        commentsMap.put("pageNum", pageNum);
        commentsMap.put("size", size);
        commentsMap.put("current", commentsList.getCurrent());

        return Result.success("获取评论列表成功", commentsMap);
    }

    @GetMapping("/getCommentTree/{articleId}")
    public Result getCommentTree(@PathVariable Integer articleId) {
        List<Comment> commentTree = commentsService.getCommentTree(articleId);

        return Result.success("获取评论树成功", commentTree);
    }

    @PostMapping("/addComment")
    public Result addComment(@RequestBody Comment comment, @RequestParam(required = false) Integer userId) {
        log.info("添加评论：{}", comment);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setStatus(1);
        comment.setIsDeleted(false);
        comment.setLikeCount(0);
        Comment parentCommentUser = comment.getParentId() != null ?
                commentsService.getById(comment.getParentId()) : null;

        Article article = articleService.getById(comment.getArticleId());
        if (article == null){
            return Result.failure(400,"攻略不存在");
        }
        boolean addComment = commentsService.addComment(comment);
        if (!addComment){
            return Result.failure(500,"评论失败");
        }
        //回复分级评论者
        if (parentCommentUser != null && parentCommentUser.getUserId() != null
                && !parentCommentUser.getUserId().equals(comment.getUserId())) {
            notificationHelper.sendNotification(comment.getUserId(), parentCommentUser.getUserId(),
                    2, comment.getArticleId(), 2, "回复了你");
        }
        //回复指定用户
        if (null != userId && userId != 0 && !userId.equals(comment.getUserId()) )
                notificationHelper.sendNotification(comment.getUserId(), userId,
                        2, comment.getArticleId(), 2, "回复了你");
        //回复攻略作者
        if (!article.getUserId().equals(comment.getUserId())
                && !article.getUserId().equals(userId)
                && (parentCommentUser == null || !article.getUserId().equals(parentCommentUser.getUserId()))) {
            notificationHelper.sendNotification(comment.getUserId(), article.getUserId(),
                    2, comment.getArticleId(), 2, "回复了你");
        }
            return Result.success("添加评论成功");

    }

    @DeleteMapping("/deleteComment/{commentId}")
    public Result deleteComment(@PathVariable Integer commentId) {
        commentsService.deleteCommentsById(commentId);
        return Result.success("删除评论成功");
    }

    @GetMapping("/getList")
    public Result getList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<Comment> page = new Page<>(pageNum, size);
        Page<Comment> commentsList = commentsService.getList(page);
        Map<String, Object> commentsMap = new HashMap<>();
        commentsMap.put("commentsList", commentsList);
        commentsMap.put("total", commentsList.getTotal());
        commentsMap.put("pages", commentsList.getPages());
        commentsMap.put("pageNum", pageNum);
        commentsMap.put("size", size);
        commentsMap.put("current", commentsList.getCurrent());
        return Result.success("获取列表成功", commentsMap);
    }

    @GetMapping("/getCommentNumber")
    public Result getCommentNumber() {
        long commentNumber = commentsService.count();
        return Result.success("获取评论数量成功", commentNumber);
    }
}
