package com.mpq.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LikeWithArticleVo {

    private Long likeId;

    private Long userId;

    private Long targetId;

    private LocalDateTime createdAt;

    private Integer targetType;

    // 文章相关信息
    private Integer articleId;

    private String title;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private String coverImage;

    private LocalDateTime articleCreatedAt;

    private String content;
}
