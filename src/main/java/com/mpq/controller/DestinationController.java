package com.mpq.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.Destination;
import com.mpq.service.DestinationService;
import com.mpq.service.MinioService;
import io.minio.errors.*;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/destination")
@RequiredArgsConstructor
public class DestinationController {
    private final DestinationService destinationService;
    private final MinioService minioService;
    private static final String bucketName = "destination";

    @GetMapping("/list")
    public Result<Map<String, Object>> getList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String province) {

        Page<Destination> page = new Page<>(pageNum, size);

        Page<Destination> result = destinationService.getList(page, province);

        Map<String, Object> map = new HashMap<>();
        map.put("list", result.getRecords());
        map.put("total", result.getTotal());
        return Result.success("获取成功", map);
    }

    @GetMapping("/{id}")
    public Result<Destination> getById(@PathVariable Integer id) {
        Destination destination = destinationService.getById(id);
        if (destination == null) {
            return Result.failure(404, "目的地不存在");
        }
        // 增加浏览量
        destinationService.incrementViewCount(id);
        return Result.success("获取成功", destination);
    }

    @GetMapping("/hot")
    public Result getHot(
            @RequestParam(defaultValue = "10") int size) {
        return Result.success("获取成功", destinationService.getHot(size));
    }

    @GetMapping("/getProvince")
    public Result getProvince() {
       Set<String> province = destinationService.getProvince();
        return Result.success("获取成功", province);
    }

    @GetMapping("/search")
    public Result search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size) {
        Page<Destination> page = new Page<>(pageNum, size);
        Page<Destination> result = destinationService.search(page, keyword);
        Map<String, Object> map = new HashMap<>();
        map.put("list", result.getRecords());
        map.put("total", result.getTotal());
        return Result.success("搜索成功", map);
    }

    @PostMapping("/add")
    public Result add(@ModelAttribute Destination destination, MultipartFile file) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            byte[] bytes = file.getBytes();
            String originalFileName = file.getOriginalFilename();
            imageUrl = minioService.uploadImage(bytes, originalFileName, contentType, bucketName);
        }
        if (destination.getName() == null) {
            return Result.failure(400, "目的地名称不能为空");
        }
        if (destination.getDescription() == null) {
            return Result.failure(400, "目的地描述不能为空");
        }
        if (destination.getLocation() == null) {
            return Result.failure(400, "目的地位置不能为空");
        }
        destination.setCoverImage(imageUrl);

        return destinationService.addDestination(destination);
    }

    @PostMapping("/update")
    public Result update(@ModelAttribute Destination destination, MultipartFile file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            byte[] bytes = file.getBytes();
            String originalFileName = file.getOriginalFilename();
            imageUrl = minioService.uploadImage(bytes, originalFileName, contentType, bucketName);
        }
        destination.setCoverImage(imageUrl);
        return destinationService.updateByDestinationId(destination);
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id) {
        boolean b = destinationService.removeById(id);
        if (b) {
            return Result.success("删除成功");
        }
        return Result.failure(500, "删除失败");
    }

    /**
     * 修改目的地浏览量
     *
     * @return Result
     */
    @PutMapping("/update")
    public Result updateViewById(Integer id) {
        return destinationService.updateViewById(id);
    }
}