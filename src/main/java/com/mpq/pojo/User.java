package com.mpq.pojo;


import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
@Schema(description = "用户基本信息")
public class User {
    @TableId(type = IdType.AUTO)
    @Schema(description = "用户ID")
    private Integer userId;

    @TableField("username")
    @Schema(description = "用户名", required = true)
    private String userName;

    @Schema(description = "邮箱", required = true)
    private String email;

    @Schema(description = "密码", required = true)
    private String password;

    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "注册时间")
    private LocalDateTime createdAt;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLogin;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "个人简介")
    private String bio;

    @Schema(description = "账号状态（1=激活，0=禁用）")
    private Integer isActive;

    @Schema(description = "评论总数")
    private Integer commentCount;

    @Schema(description = "文章总数")
    private Integer articleCount;

    @Schema(description = "关注人数")
    private Integer followerCount;

    @Schema(description = "粉丝数量")
    private Integer followingCount;

    @Schema(description = "用户角色")
    @TableField(typeHandler = org.apache.ibatis.type.EnumTypeHandler.class)
    private UserRole role;

    @TableField(exist = false)
    private String code;


}
