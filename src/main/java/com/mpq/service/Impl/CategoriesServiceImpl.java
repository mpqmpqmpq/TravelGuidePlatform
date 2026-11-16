package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.mapper.CategoriesMapper;

import com.mpq.pojo.Category;
import com.mpq.service.CategoriesService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoriesServiceImpl extends ServiceImpl<CategoriesMapper, Category> implements CategoriesService {

    private final CategoriesMapper categoriesMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_KEY_TREE = "category:tree";
    private static final String CACHE_KEY_PREFIX = "category:list:page:";
    private static final String CACHE_KEY_ID_PREFIX = "category:id:";

    @Override
    public Page<Category> getCategoryList(Page<Category> page) {
        String redisKey = CACHE_KEY_PREFIX + page.getCurrent() + ":size:" + page.getSize();

        // 优先从 Redis 获取分页数据
        Object cachedData = redisTemplate.opsForValue().get(redisKey);
        if (cachedData != null) {
            log.info("从缓存中获取分类列表成功");
            Map<String, Object> dataMap = (Map<String, Object>) cachedData;
            List<Category> records = (List<Category>) dataMap.get("records");
            Long total = (Long) dataMap.get("total");
            Long current = (Long) dataMap.get("current");
            Long size = (Long) dataMap.get("size");

            Page<Category> resultPage = new Page<>(current, size, total);
            resultPage.setRecords(records);
            return resultPage;
        }

        // 查询数据库
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        Page<Category> resultPage = categoriesMapper.selectPage(page, queryWrapper);

        // 存入 Redis
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("records", resultPage.getRecords());
        dataMap.put("total", resultPage.getTotal());
        dataMap.put("current", resultPage.getCurrent());
        dataMap.put("size", resultPage.getSize());
        redisTemplate.opsForValue().set(redisKey, dataMap, Duration.ofMinutes(30));

        log.info("从数据库中获取分类列表成功");
        return resultPage;
    }


    @Override
    public List<Category> getCategoryTree() {

        List<Category> categoryTree = (List<Category>) redisTemplate.opsForValue().get(CACHE_KEY_TREE);
        if (categoryTree != null) {
            return categoryTree;
        }
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.isNull(Category::getParentId); // 查询顶级分类
        List<Category> topLevelCategories = categoriesMapper.selectList(queryWrapper);

        // 递归加载子分类
        for (Category category : topLevelCategories) {
            loadChildren(category);
        }
        redisTemplate.opsForValue().set(CACHE_KEY_TREE, topLevelCategories, Duration.ofMinutes(30));
        return topLevelCategories;
    }


    @Override
    public boolean addCategory(Category category) {
        category.setCreatedAt(LocalDate.now());
        String slug = generateSlug(category.getName());
        category.setSlug(ensureUniqueSlug(slug));
        if (this.save(category)) {
            clearAllRelatedCaches(category.getName());
            return true;
        }
        return false;
    }

    private String ensureUniqueSlug(String slug) {
        String originalSlug = slug;
        int count = 1;
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        while (categoriesMapper.selectCount(queryWrapper.eq(Category::getSlug, slug)) > 0) {
            slug = originalSlug + "-" + count++;
        }
        return slug;
    }

    public String generateSlug(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        // 转换为小写并替换空格为连字符
        String slug = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // 去除非法字符
                .replaceAll("\\s+", "-")        // 替换空格为连字符
                .replaceAll("-+", "-");         // 合并多个连字符
        return slug;
    }


    @Transactional
    @Override
    public void deleteCategory(Integer categoryId) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getParentId, categoryId);
        Category category = categoriesMapper.selectById(categoryId);
        if (categoriesMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("该分类下有子分类，请先删除子分类");
        }
        categoriesMapper.deleteById(categoryId);
        clearAllRelatedCaches(category.getName());
    }

    @Override
    public int selectCategoryIdByName(String categoryName) {
        String nameKey = CACHE_KEY_ID_PREFIX + categoryName;

        // 优先从 Redis 中获取分类 ID
        Integer categoryId = (Integer) redisTemplate.opsForValue().get(nameKey);
        if (categoryId != null) {
            return categoryId;
        }
        // 如果 Redis 中不存在，则从数据库中查询
        categoryId = categoriesMapper.selectCategoryIdByName(categoryName);
        // 将查询结果缓存到 Redis 中（如果查询结果不为空）
        if (categoryId != null) {
            redisTemplate.opsForValue().set(nameKey, categoryId, Duration.ofMinutes(30));
        }

        return categoryId != null ? categoryId : -1; // 返回查询结果，若未找到则返回 -1
    }

    @Override
    public boolean updateCategoryById(Category category) {
        if (isCircularReference(category.getCategoryId(), category.getParentId())) {
            throw new RuntimeException("父分类不能是自身");
        }

        clearAllRelatedCaches(category.getName());
        return categoriesMapper.updateCategoryById(category);
    }

    @Override
    public Category selectById(Integer categoryId) {
        return categoriesMapper.selectById(categoryId);
    }

    private void loadChildren(Category parent) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getParentId, parent.getCategoryId());
        List<Category> children = categoriesMapper.selectList(queryWrapper);
        if (!children.isEmpty()) {
            parent.setChildren(children);
            for (Category child : children) {
                loadChildren(child);
            }
        }
    }

    private void clearAllRelatedCaches(String categoryName) {
        try {
            // 1. 清理树结构缓存
            redisTemplate.delete(CACHE_KEY_TREE);

            // 2. 清理所有分页缓存
            Set<String> pageKeys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (pageKeys != null && !pageKeys.isEmpty()) {
                redisTemplate.delete(pageKeys);
            }
            if (categoryName != null) {
                redisTemplate.delete(CACHE_KEY_ID_PREFIX + categoryName);
            }

        } catch (Exception e) {
            log.error("清理缓存失败", e);
        }
    }

    private boolean isCircularReference(Integer categoryId, Integer newParentId) {
        if (newParentId == null) {
            return false; // 如果没有父分类，则不会形成循环
        }
        if (newParentId.equals(categoryId)) {
            return true; // 直接循环：父分类等于自身
        }

        // 递归检查祖先节点
        Integer parentId = newParentId;
        while (parentId != null) {
            Category parentCategory = categoriesMapper.selectById(parentId);
            if (parentCategory == null) {
                break; // 父分类不存在，退出循环
            }
            if (parentCategory.getCategoryId().equals(categoryId)) {
                return true; // 找到循环引用
            }
            parentId = parentCategory.getParentId(); // 继续向上查找
        }
        return false;
    }


}
