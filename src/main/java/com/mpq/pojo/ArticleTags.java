package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 文章标签关联表
 * </p>
 *
 * @author mpq
 * @since 2026-03-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("article_tags")
public class ArticleTags implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文章ID
     */
    @TableId(value = "article_id", type = IdType.AUTO)
    private Integer articleId;

    /**
     * 标签ID
     */
    @TableField("tag_id")
    private Integer tagId;

    @TableField(exist = false)
    private String tagName;


}
