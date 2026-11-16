package com.mpq.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.Route;
import com.mpq.pojo.RouteSpot;
import com.mpq.service.MinioService;
import com.mpq.service.RouteService;
import com.mpq.utils.UserContextHolder;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/route")
@RequiredArgsConstructor
public class RouteController {
    private final RouteService routeService;
    private final MinioService minioService;
    private final static String BUCKET_NAME = "route-images";

    @GetMapping("/list")
    public Result getList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size) {
        Page<Route> page = new Page<>(pageNum, size);
        Page<Route> result = routeService.getList(page);
        Map<String, Object> map = new HashMap<>();
        map.put("list", result.getRecords());
        map.put("total", result.getTotal());
        return Result.success("获取成功", map);
    }

    @GetMapping("/getByUserId")
    public Result getByUserId(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size) {
        Integer userId = UserContextHolder.getUserId();
        Page<Route> page = new Page<>(pageNum, size);
        page = routeService.getByUserId(page, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("list", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        return Result.success("获取成功", result);
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id) {
        Route route = routeService.getById(id);
        if (route == null) {
            return Result.failure(404, "路线不存在");
        }
        // 增加浏览量
        routeService.incrementViewCount(id);

        // 获取路线包含的景点
        List<RouteSpot> spots = routeService.getRouteSpots(id);

        Map<String, Object> map = new HashMap<>();
        map.put("route", route);
        map.put("spots", spots);
        return Result.success("获取成功", map);
    }

    @PostMapping("/create")
    public Result create(@ModelAttribute Route route, MultipartFile file) throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String fileUrl = null;
        if (null != file && !file.isEmpty()) {
            String contentType = file.getContentType();
            String originalFileName = file.getOriginalFilename();
            fileUrl = minioService.uploadImage(file.getBytes(), originalFileName, contentType, BUCKET_NAME);
        }
        route.setCoverImage(fileUrl);

        Integer userId = UserContextHolder.getUserId();
        route.setUserId(userId.intValue());
        route.setViewCount(0);
        route.setLikeCount(0);
        Route created = routeService.create(route);
        return Result.success("创建成功", created);
    }

    @PutMapping("/{id}")
    public Result<Route> update(@PathVariable Integer id, @RequestBody Route route) {
        Route existing = routeService.getById(id);
        if (existing == null) {
            return Result.failure(404, "路线不存在");
        }

        Integer userId = UserContextHolder.getUserId().intValue();
        if (!existing.getUserId().equals(userId)) {
            return Result.failure(403, "无权修改");
        }

        route.setId(id);
        Route updated = routeService.update(route);
        return Result.success("更新成功", updated);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        Route existing = routeService.getById(id);
        if (existing == null) {
            return Result.failure(404, "路线不存在");
        }

        Integer userId = UserContextHolder.getUserId().intValue();
        if (!existing.getUserId().equals(userId)) {
            return Result.failure(403, "无权删除");
        }

        routeService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 添加路线景点
     */
    @PostMapping("/{id}/spots")
    public Result<Void> addSpot(@PathVariable Integer id, @RequestBody RouteSpot spot) {
        spot.setRouteId(id);
        routeService.addSpot(spot);
        return Result.success("添加成功");
    }

    /**
     * 删除路线景点
     */
    @DeleteMapping("/{routeId}/spots/{spotId}")
    public Result<Void> removeSpot(@PathVariable Integer routeId, @PathVariable Integer spotId) {
        routeService.removeSpot(routeId, spotId);
        return Result.success("删除成功");
    }

    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable("id") Integer routeId){
        boolean b = routeService.removeById(routeId);
        return b?Result.success("删除成功"):Result.failure(500,"删除失败");
    }




}