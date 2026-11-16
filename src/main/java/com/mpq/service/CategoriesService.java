package com.mpq.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.pojo.Category;

import java.util.List;


public interface CategoriesService extends IService<Category> {

    Page<Category> getCategoryList(Page<Category> page);
    List<Category> getCategoryTree();

    boolean addCategory(Category category);

    void deleteCategory(Integer categoryId);

    int selectCategoryIdByName(String categoryName);
    boolean updateCategoryById(Category category);

    Category selectById(Integer categoryId);
}
