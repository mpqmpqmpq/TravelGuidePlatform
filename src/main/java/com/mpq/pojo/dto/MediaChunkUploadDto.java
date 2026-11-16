package com.mpq.pojo.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

// 分片上传请求
@Data
public class MediaChunkUploadDto {
    private Integer articleId;
    private String fileName; // 初始化返回的objectName
    private String uploadId; // 分片上传ID
    private Integer chunkIndex; // 分片索引（0开始）
    private Integer totalChunks; // 总分片数
    private MultipartFile chunkFile; // 分片文件
}