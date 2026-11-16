package com.mpq.controller;

import com.mpq.common.Result;
import com.mpq.pojo.User;
import com.mpq.service.AvatarService;
import com.mpq.service.UserService;
import com.mpq.utils.UserContextHolder;
import io.minio.errors.*;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/avatar")
public class AvatarController {
    @Autowired
    private AvatarService avatarService;

    @Autowired
    private UserService userService;
    @PostMapping(value = "/upload/{userId}",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadAvatar(
            @PathVariable Integer userId,
            @RequestParam MultipartFile file) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        Integer currentUserId = UserContextHolder.getUserId();
        if(currentUserId == null || !currentUserId.equals(userId)){
            return Result.unauthorized("用户未登录");
        }
        if(file.isEmpty()){
            return Result.failure(400,"上传文件不能为空");
        }

        byte[] fileBytes = file.getBytes();
        User updateUser = avatarService.updateAvatar(
                userId,
                fileBytes,
                file.getOriginalFilename(),
                file.getContentType());
        return Result.success("上传成功",updateUser.getAvatarUrl());
    }
    @GetMapping("/{userId}/url")
    public Result<String> getAvatarUrl(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Integer userId) {

        User user = userService.getUserById(userId);
        if (user != null && StringUtils.hasText(user.getAvatarUrl())) {
            return Result.success("获取成功",user.getAvatarUrl());
        }
        return Result.failure(404,"用户不存在或未上传头像");
    }

}
