package com.mpq.pojo;

import java.math.BigDecimal;
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
 * 用户兴趣偏好表
 * </p>
 *
 * @author mpq
 * @since 2025-10-19
 */
@Data
@TableName("user_preferences")
public class UserPreferences implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 偏好记录唯一标识
     */
    @TableId(value = "preference_id", type = IdType.AUTO)
    private Long preferenceId;

    /**
     * 用户ID（关联users表）
     */
    private Integer userId;

    /**
     * 偏好类型（1=兴趣标签,2=偏好分类）
     */
    private Integer preferType;

    /**
     * 偏好值（标签名或分类ID）
     */
    private String preferValue;

    /**
     * 偏好权重（值越大兴趣越强，范围0-10）
     */
    private BigDecimal preferWeight;

    /**
     * 分类ID（prefer_type=2时）
     */
    private Integer categoryId;

    /**
     * 标签ID（prefer_type=1时）
     */
    private Integer tagId;

    /**
     * 来源（1=离线计算,2=实时计算,3=混合）
     */
    private Integer source;

    /**
     * 置信度（0-1，用于排序参考）
     */
    private BigDecimal confidence;

    /**
     * 偏好更新时间
     */
    private LocalDateTime updateTime;


}
