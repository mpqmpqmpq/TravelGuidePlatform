package com.mpq.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.config.RabbitMQConfig;
import com.mpq.mapper.ArticleMapper;
import com.mpq.pojo.Article;
import com.mpq.pojo.ArticleReview;
import com.mpq.pojo.User;
import com.mpq.pojo.dto.ArticleDto;
import com.mpq.pojo.vo.UserArticleInfoVo;
import com.mpq.utils.UserContextHolder;
import com.mpq.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/article")
public class ArticleController {
    private final UserService userService;
    private final ArticleService articleService;
    private final ArticleMapper articleMapper;
    private final MinioService minioService;
    private final RabbitTemplate rabbitTemplate;
    private final ArticleMediaService articleMediaService;
    private final ArticleReviewsService articleReviewsService;
    private final CommentsService commentsService;
    private final LikeService likeService;
    private final BookMarksService bookMarksService;
    private final ArticleTagsService articleTagsService;

    private static final String bucketName = "article-cover";
    private static final Integer STATUS_DRAFT = 0;
    private static final String ARTICLE_MEDIA_BUCKET = "article-media";

    /**
     * 预创建文章
     */
    @PostMapping("/createArticle")
    public Result<?> createArticle() {
        Integer userId = UserContextHolder.getUserId();
        Article article = new Article();
        article.setUserId(userId);
        article.setUserId(userId);
        article.setStatus(STATUS_DRAFT); // 标记为草稿，避免被查询到未发布文章
        article.setViewCount(0);
        article.setCommentCount(0);
        article.setLikeCount(0);
        article.setCreatedAt(LocalDateTime.now());
        boolean save = articleService.save(article);
        if (save) {
            return Result.success("创建成功", article.getArticleId());
        } else {
            return Result.failure(500, "创建失败");
        }

    }

    /**
     * 异步更新文章阅读量
     *
     * @param articleId 文章ID
     * @return 操作结果
     */
    @PostMapping("/viewCount")
    public Result<?> incrementViewCount(@RequestParam Integer articleId) {
        // 1. 参数校验
        if (articleId == null) {
            return Result.failure(400, "文章ID不能为空");
        }
        // 2. 校验攻略是否存在
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            return Result.failure(404, "文章不存在");
        }
        // 3. 校验是否为作者本人（作者查看不更新阅读量）
        Integer currentUserId = UserContextHolder.getUserId();
        if (article.getUserId().equals(currentUserId)) {
            return Result.success("作者本人查看，无需更新阅读量");
        }
        // 4. 调用异步方法更新阅读量
        articleService.incrementViewCount(articleId);
        return Result.success("阅读量更新请求已提交");
    }


    /**
     * 发布文章
     */
    @PostMapping("/publish")
    public Result<?> PublishArticles(@ModelAttribute ArticleDto articleDto, MultipartFile file) {

        if (articleDto.getTitle() == null || articleDto.getTitle().trim().isEmpty()) {
            return Result.failure(400, "标题不能为空");
        }
        if (articleDto.getContent() == null || articleDto.getContent().trim().isEmpty()) {
            return Result.failure(400, "内容不能为空");
        }

        try {
            Integer userId = UserContextHolder.getUserId();
            String imageUrl = null;
            if (file != null && !file.isEmpty()) { // 修复：判断文件是否为空，避免空指针
                String contentType = file.getContentType();
                byte[] fileBytes = file.getBytes();
                String originalFileName = file.getOriginalFilename();
                imageUrl = minioService.uploadImage(fileBytes, originalFileName, contentType, bucketName);
            }

            Article article = new Article();
            if (articleDto.getArticleId() != null) {
                article.setArticleId(articleDto.getArticleId());
                article.setUserId(userId);
                article.setTitle(articleDto.getTitle());
                article.setContent(articleDto.getContent());
                article.setCoverImage(imageUrl);
                article.setStatus(3); // 待审核
                article.setCreatedAt(LocalDateTime.now());
                article.setUpdatedAt(LocalDateTime.now());
                article.setViewCount(0);
                article.setCommentCount(0);
                article.setLikeCount(0);
                article.setCategoryId(articleDto.getCategoryId());
            }

            Map<String, Object> reviewTask = new HashMap<>();
            Article article1 = articleService.publishArticles(article);
            if (imageUrl != null) {
                reviewTask.put("images", new String[]{imageUrl});
            }

            reviewTask.put("articleId", article1.getArticleId());
            reviewTask.put("content", article1.getContent());
            // 发送消息给审核队列
            rabbitTemplate.convertAndSend(RabbitMQConfig.REVIEW_QUEUE, reviewTask);
            User user = userService.getById(userId);
            user.setUserId(userId);
            user.setArticleCount(user.getArticleCount() + 1);
            userService.updateUserArticleCountById(user);

            return Result.success("发布成功");
        } catch (Exception e) {
            log.error("发布失败", e);
            return Result.failure(500, "服务器内部错误，发布失败");
        }
    }

    /**
     * 获取用户文章列表
     */
    @GetMapping("/getArticleByUserId/{userId}")
    public Result<Map<String, Object>> getArticleByUserIdAndCreatedAtPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size,
            @PathVariable Integer userId,
            @RequestParam(required = false) Integer status) {
        Page<Article> page = new Page<>(pageNum, size);
        Page<Article> articlePage = articleService.getPageByUserId(page, userId, status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", articlePage.getRecords());
        result.put("total", articlePage.getTotal());
        result.put("pages", articlePage.getPages());
        result.put("current", articlePage.getCurrent());
        result.put("size", articlePage.getSize());
        return Result.success("获取成功", result);
    }

    /**
     * 获取攻略详情
     */
    @GetMapping("/getArticleById/{articleId}")
    public Result<UserArticleInfoVo> getArticleById(@PathVariable Integer articleId) {
        Article article = articleService.getArticleDetail(articleId);
        if (article == null) {
            return Result.failure(404, "攻略不存在");
        }
        User user = userService.getUserById(article.getUserId());
        log.info("userName: {}", user.getUserName());
        UserArticleInfoVo userArticleInfoVo = new UserArticleInfoVo();
        userArticleInfoVo.setArticleId(article.getArticleId());
        userArticleInfoVo.setTitle(article.getTitle());
        userArticleInfoVo.setCoverImage(article.getCoverImage());
        userArticleInfoVo.setContent(article.getContent());
        userArticleInfoVo.setViewCount(article.getViewCount()); // 缓存已实时更新
        userArticleInfoVo.setCommentCount(article.getCommentCount());
        userArticleInfoVo.setLikeCount(article.getLikeCount());
        userArticleInfoVo.setCategoryId(article.getCategoryId());
        userArticleInfoVo.setUserName(user.getUserName());
        userArticleInfoVo.setUserId(article.getUserId());
        log.info("userId：{}", article.getUserId());
        userArticleInfoVo.setAvatarUrl(user.getAvatarUrl());
        userArticleInfoVo.setCreatedAt(article.getCreatedAt());
        return Result.success("获取成功", userArticleInfoVo);
    }

    /**
     * 获取文章列表
     */
    @GetMapping("/getArticlePageList")
    public Result<Map<String, Object>> getArticlePageList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size) {
        Page<Article> articlePage = new Page<>(pageNum, size);
        articlePage = articleService.getArticlePageList(articlePage);
        Map<String, Object> result = new HashMap<>();
        result.put("list", articlePage.getRecords());
        result.put("total", articlePage.getTotal());
        result.put("pages", articlePage.getPages());
        result.put("current", articlePage.getCurrent());
        result.put("size", articlePage.getSize());
        return Result.success("获取成功", result);
    }
    @GetMapping("/getArticlePage")
    public Result<Map<String, Object>> getArticlePage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size) {
        Page<Article> articlePage = new Page<>(pageNum, size);
        articlePage = articleService.getArticle(articlePage);
        Map<String, Object> result = new HashMap<>();
        result.put("list", articlePage.getRecords());
        result.put("total", articlePage.getTotal());
        result.put("pages", articlePage.getPages());
        result.put("current", articlePage.getCurrent());
        result.put("size", articlePage.getSize());
        return Result.success("获取成功", result);
    }

    /**
     * 阅读量更新接口
     */
    @PostMapping("/updateArticle")
    public Result updateArticleView(@RequestBody Article article) {
        try {
            // 1. 参数校验（原逻辑未校验）
            if (article == null || article.getArticleId() == null) {
                return Result.failure(400, "文章ID不能为空");
            }

            // 2. 校验文章是否存在
            Article existArticle = articleService.getArticleDetail(article.getArticleId());
            if (existArticle == null) {
                return Result.failure(404, "文章不存在");
            }

            // 3. 校验是否为作者本人
            Integer currentUserId = UserContextHolder.getUserId();
            if (existArticle.getUserId().equals(currentUserId)) {
                return Result.success("作者本人查看，无需更新阅读量");
            }

            // 4. 内部调用新的异步方法
            articleService.incrementViewCount(article.getArticleId());
            return Result.success("阅读量更新成功");
        } catch (Exception e) {
            log.error("旧阅读量接口更新失败", e);
            return Result.failure(500, "服务器内部错误，更新失败");
        }
    }

    /**
     * 根据分类id获取文章列表
     */
    @GetMapping("/getArticlePageListByCategoryId")
    public Result getArticlePageListByCategoryId(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Integer categoryId) {
        Page<Article> articlePage = new Page<>(pageNum, size);
        articlePage = articleService.getArticlePageListByCategoryId(articlePage, categoryId);
        Map<String, Object> result = new HashMap<>();
        result.put("list", articlePage.getRecords());
        result.put("total", articlePage.getTotal());
        result.put("pages", articlePage.getPages());
        result.put("current", articlePage.getCurrent());
        result.put("size", articlePage.getSize());
        return Result.success("获取成功", result);
    }

    /**
     * 修改文章状态
     */
    @PutMapping("/status")
    public Result updateArticleStatus(@RequestBody Article article) {
        // 修复：补充ArticleId校验（原逻辑未校验，可能导致空指针）
        if (article == null || article.getArticleId() == null || article.getStatus() == null) {
            return Result.failure(400, "文章ID和状态不能为空");
        }
        boolean b = articleService.updateArticleStatus(article);
        if (b) {
            return Result.success("修改成功");
        }
        return Result.failure(500, "修改失败");
    }

    /**
     * 获取热门文章
     */
    @GetMapping("/getHotArticles")
    public Result getHotArticles() {
        List<Article> hotArticles = articleService.getHotArticles();
        return Result.success("获取成功", hotArticles);
    }

    @DeleteMapping("/deleteArticle/{articleId}")
    public Result deleteArticle(@PathVariable Integer articleId) {

        try {
            // 1. 校验文章是否存在
            Article article = articleService.getById(articleId);
            if (article == null) {
                return Result.failure(404, "文章不存在或已被删除");
            }

            // 2. 权限校验：仅作者可删除
            Integer currentUserId = UserContextHolder.getUserId();
            if (!article.getUserId().equals(currentUserId)) {
                return Result.failure(403, "无权删除他人的文章");
            }

            // 3. 删除关联媒体（封面+内容中的图片/视频）
            // 3.1 删除文章封面
            if (article.getCoverImage() != null) {
                minioService.deleteImage(article.getCoverImage(), bucketName);
            }
            // 3.2 删除所有关联的媒体文件
            articleMediaService.deleteByArticleId(articleId);

            // 4. 删除文章本身
            articleService.deleteById(articleId, currentUserId);
            ArticleReview articleReview = articleReviewsService.getByArticleId(articleId);
            if (articleReview != null) {
                articleReviewsService.removeById(articleReview.getArticleId());
            }
            commentsService.deleteByArticleId(articleId);
            likeService.deleteByArticleId(articleId);
            bookMarksService.deleteBookMarksByArticleId(articleId);
            articleTagsService.deleteByArticleId(articleId);
            log.info("文章删除成功，articleId: {}, 作者: {}", articleId, currentUserId);
            return Result.success("文章及关联媒体删除成功");

        } catch (Exception e) {
            log.error("删除文章失败，articleId: {}", articleId, e);
            return Result.failure(500, "删除失败：" + e.getMessage());
        }
    }

    @GetMapping("/getArticleNumber")
    public Result getArticleNumber() {
        long articleNumber = articleService.count();
        return Result.success("获取成功", articleNumber);
    }

    @GetMapping("/getArticleAllView")
    public Result getArticleAllView() {
        long articleAllView = articleService.getArticleAllView();
        return Result.success("获取成功", articleAllView);
    }

    @GetMapping("/search")
    public Result<Map<String, Object>> searchArticles(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "default") String sort) {
        Page<Article> page = new Page<>(pageNum, size);
        Page<Article> result = articleService.searchArticles(page, keyword, sort);
        Map<String, Object> map = new HashMap<>();
        map.put("list", result.getRecords());
        map.put("total", result.getTotal());
        return Result.success("搜索成功", map);
    }
}