package com.mpq.controller;

import com.mpq.common.Result;
import com.mpq.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
@Slf4j
@RequiredArgsConstructor
public class ImageUploadController {

    private final MinioService minioService;
    
    private static final String IMAGE_BUCKET = "travel";

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadImage(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return Result.failure(400, "上传文件不能为空");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.failure(400, "只支持上传图片文件");
        }

        try {
            byte[] fileBytes = file.getBytes();
            String originalFileName = file.getOriginalFilename();
            
            String imageUrl = minioService.uploadImage(
                fileBytes, 
                originalFileName, 
                contentType, 
                IMAGE_BUCKET
            );

            log.info("上传图片成功，URL: {}", imageUrl);
            return Result.success("上传成功", imageUrl);

        } catch (Exception e) {
            log.error("上传图片失败", e);
            return Result.failure(500, "上传失败: " + e.getMessage());
        }
    }
}
