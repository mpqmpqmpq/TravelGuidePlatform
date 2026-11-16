package com.mpq.pojo.dto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RegisterDto {
    private Integer userId;

    private String userName;

    private String email;

    private String password;

    private String verifyCode; // 验证码字段

    private LocalDateTime createdAt;

    private LocalDateTime lastLogin;

    private String avatarUrl;

    private String bio;

    private Integer isActive;
}
