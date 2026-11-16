package com.mpq.controller;

import com.mpq.common.Result;
import com.mpq.pojo.User;
import com.mpq.pojo.dto.RegisterDto;
import com.mpq.service.EmailService;
import com.mpq.service.MinioService;
import com.mpq.service.UserService;
import com.mpq.utils.PasswordEncoderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/registers")
@Slf4j
@RequiredArgsConstructor
public class RegisterController {

    private final MinioService minioService;

    private final EmailService emailService;


    private final UserService userService;

    private static final String BUCKET_NAME = "user-avatars";

    @Value("${default.avatar}")
    private String defaultAvatarFileName;

    @PostMapping("/register")
    public Result register(
            @ModelAttribute RegisterDto registerDto,
            MultipartFile file) {
        if (registerDto.getUserName() == null) {
            return Result.failure(400, "用户名不能为空");
        }
        if (registerDto.getPassword() == null) {
            return Result.failure(400, "密码不能为空");
        }
        if (registerDto.getEmail() == null) {
            return Result.failure(400, "邮箱不能为空");
        }
        User userByEmail = userService.getUserByEmail(registerDto.getEmail());
        if (userByEmail != null) {
            return Result.failure(400, "邮箱已被注册");
        }
        if (!emailService.verifyCode(registerDto.getEmail(), registerDto.getVerifyCode())) {
            return Result.failure(400, "验证码错误");
        }
        try {
            String avatarUrl;
            if (file != null) {
                String contentType = file.getContentType();
                byte[] fileBytes = file.getBytes();
                String avatarFileName = minioService.generateAvatarFileName(file.getOriginalFilename());
                avatarUrl = minioService.uploadImage(fileBytes, avatarFileName, contentType, BUCKET_NAME);
            } else {
                avatarUrl = defaultAvatarFileName;
            }
            if (avatarUrl == null) {
                return Result.failure(500, "上传头像失败");
            }
            registerDto.setAvatarUrl(avatarUrl);
            registerDto.setPassword(PasswordEncoderUtil.encode(registerDto.getPassword()));
            registerDto.setEmail(registerDto.getEmail());
            userService.register(registerDto);
            log.info("用户名：{}", registerDto.getUserName());
            log.info("用户注册");
            return Result.success("注册成功");
        } catch (Exception e) {
            log.error("注册失败", e);
            return Result.failure(500, "注册失败");
        }
    }

    /**
     * 发送验证码
     */
    @PostMapping("/sendCode")
    public Result sendCode(@RequestParam String email) {
        try {
            User userByEmail = userService.getUserByEmail(email);
            if (userByEmail != null) {
                return Result.failure(400, "邮箱已存在");
            }
            emailService.sendVerificationCode(email);
            return Result.success("验证码已发送至邮箱");
        } catch (Exception e) {
            log.error("发送验证码失败", e);
            return Result.failure(500, "发送验证码失败");
        }
    }


}
