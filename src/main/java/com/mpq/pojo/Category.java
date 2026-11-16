package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@TableName("categories")
@Schema(description = "文章分类")
public class Category {
    @TableId(type = IdType.AUTO)
    @Schema(description = "分类ID")
    private Integer categoryId;
    
    @Schema(description = "分类名称", required = true)
    private String name;
    
    @Schema(description = "URL友好标识", required = true)
    private String slug;
    
    @Schema(description = "父分类ID")
    private Integer parentId;
    
    @Schema(description = "文章数量")
    private Integer articleCount;
    
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDate createdAt;

    @TableField(exist = false)
    private List<Category> children;

}
