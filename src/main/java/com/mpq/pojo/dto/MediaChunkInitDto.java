package com.mpq.pojo.dto;

import lombok.Data;

// 分片上传初始化请求
@Data
public class MediaChunkInitDto {
    private Integer articleId;
    private String fileName;
    private String contentType;
    private Long fileSize; // 文件总大小（字节）
    private String fileMd5; // 文件唯一标识（用于校验）
}