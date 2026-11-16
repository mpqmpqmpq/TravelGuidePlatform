package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 景点图片表
 * </p>
 *
 * @author mpq
 * @since 2026-05-11
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("scenic_images")
public class ScenicImages implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "image_id", type = IdType.AUTO)
    private Long imageId;

    /**
     * 所属景点ID
     */
    @TableField("scenic_id")
    private Long scenicId;

    /**
     * 图片URL
     */
    @TableField("image_url")
    private String imageUrl;

    /**
     * 排序，越小越靠前
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 是否封面（0否1是）
     */
    @TableField("is_cover")
    private Integer isCover;

    @TableField("created_at")
    private LocalDateTime createdAt;


}
