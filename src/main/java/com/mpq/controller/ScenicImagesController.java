package com.mpq.controller;


import com.mpq.common.Result;
import com.mpq.pojo.ScenicImages;
import com.mpq.service.MinioService;
import com.mpq.service.ScenicImagesService;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * <p>
 * 景点图片表
 * </p>
 *
 * @author mpq
 * @since 2026-05-11
 */
@RestController
@RequestMapping("/scenic-images")
@RequiredArgsConstructor
public class ScenicImagesController {
    private final ScenicImagesService scenicImagesService;
    private final MinioService minioService;
    private static final String bucketName = "scenic-spots";

    @GetMapping("/list/{scenicId}")
    public Result list(@PathVariable Integer scenicId) {
        List<ScenicImages> list = scenicImagesService.getByScenicId(scenicId);
        return Result.success("获取成功", list);
    }

    @PostMapping("/add")
    public Result addImages(@RequestBody ScenicImages image) {
        image.setIsCover(0);
        boolean result = scenicImagesService.save(image);
        return result ? Result.success("添加成功") : Result.failure(500, "添加失败");

    }

    @PostMapping("/addBatch")
    public Result addBatch(@RequestBody List<ScenicImages> images) {
        scenicImagesService.saveBatch(images);
        return Result.success("添加成功");
    }

    @DeleteMapping("/delete/{imageId}")
    public Result delete(@PathVariable Long imageId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        ScenicImages image = scenicImagesService.getById(imageId);
        scenicImagesService.removeById(imageId);
        minioService.deleteImage(image.getImageUrl(), bucketName);
        return Result.success("删除成功");
    }

    @PutMapping("/update")
    public Result update(@RequestBody ScenicImages scenicImages) {
        scenicImagesService.updateById(scenicImages);
        return Result.success("更新成功");
    }

    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String imageUrl = null;
        if (!file.isEmpty()) {
            String contentType = file.getContentType();
            byte[] bytes = file.getBytes();
            String originalFileName = file.getOriginalFilename();
            imageUrl = minioService.uploadImage(bytes, originalFileName, contentType, bucketName);
        }
        return Result.success("上传成功", imageUrl);
    }

    @DeleteMapping("/deleteImage")
    public Result deleteImage(@RequestParam String imageUrl) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        minioService.deleteImage(imageUrl, bucketName);
        return Result.success("删除成功");
    }
}
