package com.mpq.pojo.dto;

import lombok.Data;

import java.util.List;

//  分片合并请求
@Data
public class MediaChunkCompleteDto {
    private Integer articleId;
    private String fileName; // 初始化返回的objectName
    private String uploadId;
    private List<String> etagList; // 各分片ETag
    private String mediaTitle; // 媒体标题
    private Integer position; // 展示位置
    private String contentType; // 文件MIME类型（如image/jpeg、video/mp4，用于区分媒体类型）
    private String extraInfo;
}