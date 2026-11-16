package com.mpq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mpq.pojo.UserPreferences;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreferences> {
    @Delete("delete from user_preferences where user_id = #{userId}")
    void deleteByUserId(Integer userId);


    @Select("select preference_id, user_id, prefer_type, prefer_value, prefer_weight, update_time from user_preferences where user_id = #{userId}")
    List<UserPreferences> selectPreferValueByUserId(Integer userId);
}
