package com.mpq.pojo.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@RequiredArgsConstructor
public class ScenicReviewsAndUserVo {

    private Integer reviewId;


    private Integer scenicId;


    private Integer userId;


    private BigDecimal rating;


    private String content;


    private String images;


    private Integer status;


    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long replyTo;

    private String username;

    private String avatarUrl;

}
