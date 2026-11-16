package com.mpq.pojo.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@RequiredArgsConstructor

public class UserArticleInfoVo {
    private Integer articleId;
    private String title;
    private String coverImage;
    private String content;
    private Integer viewCount;
    private Integer commentCount;
    private Integer likeCount;
    private Integer categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
    private String userName;
    private Integer userId;
    private String avatarUrl;
}
