package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户行为原始日志表
 * </p>
 *
 * @author mpq
 * @since 2026-05-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user_behavior_log")
public class UserBehaviorLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "log_id", type = IdType.AUTO)
    private Integer logId;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 会话ID
     */
    @TableField("session_id")
    private String sessionId;

    /**
     * 1=文章
     */
    @TableField("target_type")
    private Integer targetType;

    /**
    *被交互的主键id
    * */
    @TableField("target_id")
    private Integer targetId;

    /**
     *  2=点击，3=收藏，4=点赞，
     */
    @TableField("action_type")
    private Integer actionType;

    @TableField("action_value")
    private Integer actionValue;

    @TableField("recall_source")
    private Integer recallSource;

    @TableField("rec_position")
    private Integer recPosition;

    @TableField("extra")
    private String extra;

    @TableField("created_at")
    private LocalDateTime createdAt;


}
