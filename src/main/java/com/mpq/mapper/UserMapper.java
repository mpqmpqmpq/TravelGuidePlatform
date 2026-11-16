package com.mpq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.pojo.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("select user_id, password,avatar_url,role from users where username=#{userName} ")
    User selectByUserName(String userName);

    @Select("select username,email,avatar_url,bio,article_count,follower_count,following_count,last_login,created_at,comment_count from users where user_id = #{id}")
    User selectById(Integer id);

//    @Select("select count(1) from users where phone = #{phone} and is_active != 0")
//    int selectByPhone(String phone);

    int updateById(User user);

    @Insert("insert into users(user_id, username, email, password, created_at, last_login, avatar_url, bio, is_active, comment_count, article_count, follower_count, following_count) " +
            "VALUES (#{userId}, #{userName}, #{email}, #{password}, #{createdAt}, #{lastLogin}, #{avatarUrl}, #{bio}, #{isActive}, #{commentCount}, #{articleCount}, #{followerCount}, #{followingCount})")
    int insert(User user);

    @Update("update users set is_active =0  where user_id = #{userId}")
    int deleteById(int userId);

    @Select("select user_id, password,avatar_url,role from users where user_id=#{currentId} ")
    User selectCurrentById(Integer currentId);

    Page<User> searchUsers(Page<User> page, @Param("keyword") String keyword);

    @Select("select username, user_id, password,avatar_url,role from users where email= #{email} ")
    User selectByEmail(String email);
}
