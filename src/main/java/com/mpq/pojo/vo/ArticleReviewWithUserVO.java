package com.mpq.pojo.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ArticleReviewWithUserVO {
    // 审核ID
    private Integer reviewId;
    
    // 文章ID
    private Integer articleId;
    
    // 文章标题
    private String title;
    
    // 作者
    private String userName;
    
    // 审核状态
    private Integer status;
    
    // 审核意见
    private String reason;
    
    // 提交时间
    private LocalDateTime createdAt;
    
    // 审核时间
    private LocalDateTime reviewedAt;
}