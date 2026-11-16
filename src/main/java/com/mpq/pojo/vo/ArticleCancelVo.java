package com.mpq.pojo.vo;


import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
@Data
@RequiredArgsConstructor
public class ArticleCancelVo {

    private Integer BookMarksId;
    private Integer articleId;
    private Integer userId;

    private String title;

    private String content;

    private String coverImage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer viewCount;

    private Integer commentCount;

    private Integer likeCount;

    private Integer categoryId;
}
