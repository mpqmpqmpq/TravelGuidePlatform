package com.mpq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mpq.pojo.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CategoriesMapper extends BaseMapper<Category> {

    @Select("select category_id from try_my_ai.categories where name = #{categoryName}")
    int selectCategoryIdByName(String categoryName);

    boolean updateCategoryById(Category category);

}
