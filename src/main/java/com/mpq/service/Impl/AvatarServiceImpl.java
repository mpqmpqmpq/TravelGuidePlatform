package com.mpq.service.Impl;

import com.mpq.mapper.UserMapper;
import com.mpq.pojo.User;
import com.mpq.service.AvatarService;
import com.mpq.service.MinioService;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class AvatarServiceImpl implements AvatarService {
    @Autowired
    private MinioService minioService;
    @Autowired
    private UserMapper userMapper;

    private static final String AVATAR_BUCKET_NAME = "user-avatars";

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    // 允许的文件扩展名
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    @Transactional
    public User updateAvatar(Integer userId, byte[] fileBytes, String originalFilename, String contentType) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setUserId(userId);
//       验证头像内容
        validateAvatarContent(fileBytes, originalFilename, contentType);
//       删除就头像
        deleteAvatar(user);

//        上传更新头像
        String newAvatarUrl = minioService.uploadImage(
                fileBytes,
                originalFilename,
                contentType,
                AVATAR_BUCKET_NAME
        );
        user.setAvatarUrl(newAvatarUrl);
        userMapper.updateById(user);
        return user;


    }

    @Override
    public void validateAvatarContent(byte[] fileBytes, String originalFilename, String contentType) {
//        验证文件类型
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("不支持的文件类型: " + contentType);
        }

        // 验证文件扩展名
        String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件扩展名: " + extension);
        }

        // 验证文件大小 (最大2MB)
        if (fileBytes.length > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("文件大小不能超过2MB");
        }

        // 验证文件内容是否为空
        if (fileBytes.length == 0) {
            throw new IllegalArgumentException("文件内容为空");
        }
    }

    @Override
    public void deleteAvatar(User user) {
        if (StringUtils.hasText(user.getAvatarUrl())) {
            try {
                minioService.deleteImage(user.getAvatarUrl(), AVATAR_BUCKET_NAME);
                log.info("删除旧头像: {}", user.getAvatarUrl());
            } catch (Exception e) {
                log.error("删除旧头像失败: {}", user.getAvatarUrl(), e);
            }
        }

    }
}
