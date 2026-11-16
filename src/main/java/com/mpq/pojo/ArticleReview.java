package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_reviews")
public class ArticleReview {
    @TableId(type = IdType.AUTO)
    private Integer reviewId;
    private Integer articleId;
    private Integer status; // 审核状态：1=审核中, 2=通过, 3=不通过
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private String reason; // 审核理由
}