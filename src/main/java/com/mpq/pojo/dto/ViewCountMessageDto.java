package com.mpq.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 阅读量更新消息载体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewCountMessageDto {
    private Integer articleId; // 文章ID
    private Integer increment; // 增量（固定为1）
    private String messageId;  // 消息唯一ID（幂等性校验）
}