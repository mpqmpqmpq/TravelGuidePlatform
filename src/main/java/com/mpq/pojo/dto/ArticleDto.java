package com.mpq.pojo.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ArticleDto {
    private Integer articleId;
    private String title;
    private String content;
    private Integer categoryId;

}
