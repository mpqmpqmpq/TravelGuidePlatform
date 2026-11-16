package com.mpq.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.ScenicSpots;
import com.mpq.service.MinioService;
import com.mpq.service.ScenicSpotsService;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 景点信息表
 * </p>
 *
 * @author mpq
 * @since 2026-03-20
 */
@RestController
@RequestMapping("/scenic-spots")
@RequiredArgsConstructor
public class ScenicSpotsController {
    private final ScenicSpotsService scenicSpotsService;
    private final MinioService minioService;

    private static final String bucketName = "scenic-spots-cover";

    @GetMapping("/list")
    public Result getScenicSpotsList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String location
    ) {
        Page<ScenicSpots> pageResult = new Page<>(pageNum, size);

        if (location == null) {
            pageResult = scenicSpotsService.getScenicSpotsList(pageResult);
        }
        if (location != null) {
            pageResult = scenicSpotsService.getScenicSpotsListByLocaltion(pageResult, location);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("pages", pageResult.getPages());
        result.put("current", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return Result.success("查询成功", result);
    }


    @PostMapping("/add")
    public Result addScenicSpots(@ModelAttribute ScenicSpots scenicSpots, MultipartFile file) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (scenicSpots.getName() == null) {
            return Result.failure(400, "景点名称不能为空");
        }
        if (scenicSpots.getDescription() == null)
            return Result.failure(400, "景点描述不能为空");
        if (scenicSpots.getLocation() == null)
            return Result.failure(400, "景点位置不能为空");
        if (scenicSpots.getTicketPrice() == null)
            return Result.failure(400, "景点门票价格不能为空");
        if (scenicSpots.getOpenHours() == null)
            return Result.failure(400, "景点开放时间不能为空");
        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            byte[] bytes = file.getBytes();
            String originalFileName = file.getOriginalFilename();
            imageUrl = minioService.uploadImage(bytes, originalFileName, contentType, bucketName);
        }
        scenicSpots.setCoverImage(imageUrl);
        return scenicSpotsService.addScenicSpots(scenicSpots);

    }

    @PutMapping("/update")
    public Result updateScenicSpots(@ModelAttribute ScenicSpots scenicSpots, MultipartFile file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String imageUrl = null;
        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            byte[] bytes = file.getBytes();
            String originalFileName = file.getOriginalFilename();
            imageUrl = minioService.uploadImage(bytes, originalFileName, contentType, bucketName);
        }
        scenicSpots.setCoverImage(imageUrl);
        return scenicSpotsService.updateScenicSpots(scenicSpots);
    }

    @PutMapping("/updateViews")
    public Result updateViews(@RequestParam Integer id) {
        return scenicSpotsService.updateViews(id);
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteScenicSpots(@PathVariable Integer id) {
        return scenicSpotsService.deleteScenicSpots(id);
    }

    @GetMapping("/get/{id}")
    public Result getScenicSpots(@PathVariable Integer id) {
        return Result.success("查询成功", scenicSpotsService.getScenicSpotsById(id));
    }

    @GetMapping("/getByConditional")
    public Result getByConditional(@RequestParam(defaultValue = "1") int pageNum,
                                   @RequestParam(defaultValue = "10") int size,
                                   @RequestParam(required = false) BigDecimal ticketPrice,
                                   @RequestParam(required = false) String sortBy) {
        Page<ScenicSpots> page = new Page<>(pageNum, size);
        Page<ScenicSpots> pageResult = scenicSpotsService.getByConditional(page, ticketPrice, sortBy);
        return Result.success("查询成功", pageResult);
    }

    @GetMapping("/getHotScenicSpots")
    public Result getHotScenicSpots(@RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "10") int size) {
        Page<ScenicSpots> page = new Page<>(pageNum, size);
        Page<ScenicSpots> pageResult = scenicSpotsService.getHotScenicSpots(page);
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("pages", pageResult.getPages());
        result.put("current", pageResult.getCurrent());
        result.put("size", pageResult.getSize());
        return Result.success("查询成功", result);
    }

    @GetMapping("/search")
    public Result<Map<String, Object>> searchSpots(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size) {
        Page<ScenicSpots> page = new Page<>(pageNum, size);
        Page<ScenicSpots> result = scenicSpotsService.searchSpots(page, keyword);
        Map<String, Object> map = new HashMap<>();
        map.put("list", result.getRecords());
        map.put("total", result.getTotal());
        return Result.success("搜索成功", map);
    }
}
