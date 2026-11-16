package com.mpq.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.Category;
import com.mpq.service.CategoriesService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
@Slf4j
public class CategoriesController {
    private final CategoriesService categoriesService;
    @GetMapping("/getCategoryList")
    public Result<Map<String, Object>> getCategoryList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<Category> page = new Page<>(pageNum, size);
       page =  categoriesService.getCategoryList(page);
        Map<String, Object> categoryMap = new HashMap<>();
        categoryMap.put("list", page.getRecords());
        categoryMap.put("pages", page.getPages());
        categoryMap.put("total", page.getTotal());
        categoryMap.put("current", page.getCurrent());
        categoryMap.put("size", page.getSize());
        return Result.success("获取成功", categoryMap);
    }
    @GetMapping("/getCategoryTree")
    public Result<Map<String, Object>> getCategoryTree() {
        Map<String, Object> categoryMap = new HashMap<>();
        categoryMap.put("list", categoriesService.getCategoryTree());
        return Result.success("获取成功", categoryMap);
    }
    @PostMapping("/addCategory")
    public Result<String> addCategory(@RequestBody Category category) {
        log.info("添加分类:"+category);
        try {
            if (category != null) {
                if (categoriesService.addCategory(category)) {
                    log.info("添加成功");
                    return Result.success("添加成功");
                }
            }
            return Result.failure(500, "参数不能为空");
        }catch (Exception e) {
            log.error("系统异常:"+e.getMessage());
            return Result.failure(500, "系统异常:"+e.getMessage());
        }
    }
    @DeleteMapping("/deleteCategory/{categoryId}")
    public Result<String> deleteCategory(@PathVariable Integer categoryId)
    {
        log.info("删除分类:"+categoryId);
        try {
            categoriesService.deleteCategory(categoryId);
            return Result.success("删除成功");
        }catch (Exception e){
            log.error("系统异常:"+e.getMessage());
            return Result.failure(500, e.getMessage());
        }
    }
    @GetMapping("/selectCategoryIdByName")
    public Result selectCategoryIdByName(String categoryName){

         int categoryId = categoriesService.selectCategoryIdByName(categoryName);
        return Result.success("查询成功",categoryId);
    }
    @PutMapping("/updateCategory")
    public  Result updateCategoryById(@RequestBody Category category){
        if (category == null){
            return Result.failure(500, "参数不能为空");
        }
        categoriesService.updateCategoryById(category);
        return Result.success("更新成功");
    }
}
