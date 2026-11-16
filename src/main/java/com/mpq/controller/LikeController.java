package com.mpq.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.Article;
import com.mpq.pojo.vo.LikeWithArticleVo;
import com.mpq.service.ArticleService;
import com.mpq.service.LikeService;
import com.mpq.utils.NotificationHelper;
import com.mpq.utils.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/like")
@RequiredArgsConstructor
public class LikeController {
    private final LikeService likeService;
    private final NotificationHelper notificationHelper;
    private final ArticleService articleService;
    /**
     * 获取点赞文章列表
     */
    @GetMapping("/list")
    public Result getLikeWithArticleList(
          @RequestParam(defaultValue = "1") Integer pageNum,
          @RequestParam(defaultValue = "10")  Integer pageSize
    ){ 
        Integer userId = UserContextHolder.getUserId();
        Page<LikeWithArticleVo> page = new Page<>(pageNum,pageSize);
       return likeService.selectLikeWithArticle(page,userId);
    }
    /**
     * 点赞文章
     */
    @PostMapping("/article")
    public Result likeArticle(@RequestParam Integer articleId){
        Integer userId = UserContextHolder.getUserId();
        boolean like = likeService.likeArticle(articleId,userId);
        if(!like){
            return Result.failure(500,"点赞失败");
        }
        Article article = articleService.getById(articleId);

        notificationHelper.sendNotification(userId,article.getUserId(),1,articleId,1,"点赞了你的攻略");
        return  Result.success("点赞成功") ;
    }
   /**
     * 取消点赞文章
     */
    @DeleteMapping("/article/{articleId}")
    public Result cancelLikeArticle(@PathVariable Integer articleId){
        Integer userId = UserContextHolder.getUserId();
        boolean cancelLike = likeService.cancelLikeArticle(articleId,userId);
        return cancelLike ? Result.success("取消点赞成功") : Result.failure(500,"取消点赞失败");
    }

    /**
     * 点赞评论
     * @param commentId
     * @return
     */
    @PostMapping("/comment")
    public Result likeComment(@RequestParam Integer commentId){
        Integer userId = UserContextHolder.getUserId();
        boolean like = likeService.likeComment(commentId,userId);
        return like ? Result.success("点赞成功") : Result.failure(500,"点赞失败");
    }


    /**
     * 取消点赞评论
     * @param commentId
     * @return
     */
    @DeleteMapping("/comment/{commentId}")
    public Result cancelLikeComment(@PathVariable Integer commentId){
        Integer userId = UserContextHolder.getUserId();
        boolean cancelLike = likeService.cancelLikeComment(commentId,userId);
        return cancelLike ? Result.success("取消点赞成功") : Result.failure(500,"取消点赞失败");
    }

    @GetMapping("/article/status/{articleId}")
   public Result getArticleLikeStatus(@PathVariable Integer articleId) {
              Integer userId = UserContextHolder.getUserId();
              return likeService.getArticleLikeStatus(articleId, userId);
    }

}
