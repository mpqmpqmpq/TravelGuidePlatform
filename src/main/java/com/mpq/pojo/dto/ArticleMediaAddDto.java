package com.mpq.pojo.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ArticleMediaAddDto  {
    private Integer articleId;              // 要关联的文章ID（必传）
    private MultipartFile mediaFile;     // 媒体文件（图片/视频，必传）
    private Integer position;            // 插入位置（默认0，可选）
    private String mediaTitle;           // 媒体标题（可选）
    private String extraInfo;            // 额外信息（如视频分辨率，可选）
}