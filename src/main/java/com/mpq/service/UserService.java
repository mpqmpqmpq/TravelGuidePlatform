package com.mpq.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.pojo.User;
import com.mpq.pojo.dto.RegisterDto;
import com.mpq.pojo.dto.UpdatePasswordDto;

public interface UserService extends IService<User> {

    User login(String userName,String password);

    User getUserById(Integer id);

    void register(RegisterDto registerDto);

    User updateUserById(User user);

    Integer updatePasswordById(UpdatePasswordDto passwordDto);

    boolean deleteUserById(int userId);

    boolean verifyPassword(int userId, String password);

    Page<User> getUserList(Page<User>  page);

    User getCurrentById(Integer currentId);

    Boolean updateUserStatusById(Integer id);

    Page<User> searchUsers(Page<User> page, String keyword);

    User getUserByEmail(String trim);

    void updateUserArticleCountById(User user);

    void addOnline(Integer userId);

    long getUserOnline();

    void removeOnline(Integer userId);
}
