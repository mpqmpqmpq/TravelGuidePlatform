package com.mpq.service.Impl;


import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.mpq.mapper.UserMapper;
import com.mpq.pojo.User;
import com.mpq.pojo.UserRole;
import com.mpq.pojo.dto.RegisterDto;
import com.mpq.pojo.dto.UpdatePasswordDto;
import com.mpq.service.UserService;
import com.mpq.utils.PasswordEncoderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;

import org.springframework.beans.BeanUtils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    private  final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private static final String ONLINE_KEY = "online:users";
    
    @Override
    public User login(String userName, String password) {
        User user = userMapper.selectByUserName(userName);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setLastLogin(LocalDateTime.now());
        if (!PasswordEncoderUtil.matches(password, user.getPassword())) {
            return null;
        }

        userMapper.updateById(user);

        return user;
    }

    @Override
    public User getUserById(Integer id) {
        User user = userMapper.selectById(id);
        if (user == null){
            return null;
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public void register(RegisterDto registerDto) {
        User login = userMapper.selectByUserName(registerDto.getUserName());
        if (login != null) {
            throw new RuntimeException("该用户名被注册");
        }
        User user = new User();
        BeanUtils.copyProperties(registerDto, user);
        user.setBio("");
        user.setArticleCount(0);
        user.setCommentCount(0);
        user.setFollowerCount(0);
        user.setFollowingCount(0);
        user.setRole(UserRole.user);
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        user.setIsActive(1);
        userMapper.insert(user);
    }


    @Override
    public User getUserByEmail(String email) {
        User user = userMapper.selectByEmail(email);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public void updateUserArticleCountById(User user) {
        userMapper.updateById(user);
    }

    @Override
    public void addOnline(Integer userId) {
        long nowSec = System.currentTimeMillis()/1000;
        redisTemplate.opsForZSet().add(ONLINE_KEY, String.valueOf(userId),nowSec);
    }

    @Override
    public long getUserOnline() {
        Long size = redisTemplate.opsForZSet().size(ONLINE_KEY);
        return size != null ? size : 0L;
    }

    @Override
    public void removeOnline(Integer userId) {
        redisTemplate.opsForZSet().remove(ONLINE_KEY,String.valueOf(userId));
    }

    @Scheduled(fixedRate = 5*60*1000)
    public void cleanExpiredUsers(){
        long nowSec = System.currentTimeMillis()/1000;
        long threshold = nowSec -1800;
        redisTemplate.opsForZSet().removeRangeByScore(ONLINE_KEY,0,threshold);
    }

    @Override
    @Transactional
    public User updateUserById(User user) {
        int i = userMapper.updateById(user);
        if (i == 0) {
            throw new RuntimeException("更新用户信息失败");
        }
        User userUpdate = userMapper.selectById(user.getUserId());
        return userUpdate;
    }

    @Override
    @Transactional
    public Integer updatePasswordById(UpdatePasswordDto passwordDto) {
        User user = userMapper.selectById(passwordDto.getUserId());
        boolean matches = PasswordEncoderUtil.matches(passwordDto.getOldPassword(), user.getPassword());
        if (matches) {
            user.setPassword(PasswordEncoderUtil.encode(passwordDto.getNewPassword()));
            int i = userMapper.updateById(user);
            if (i == 0) {
                return 0;
            }
        } else {
            return 2;
        }
        return 1;
    }

    //    删除用户
    @Override
    public boolean deleteUserById(int userId) {
        int i = userMapper.deleteById(userId);
        if (i != 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean verifyPassword(int userId, String password) {
        User user = userMapper.selectById(userId);
        log.info("用户密码：" + user.getPassword());
        log.info("输入密码：" + password);
        return PasswordEncoderUtil.matches(password, user.getPassword());
    }

    @Override
    public Page<User> getUserList(Page<User> page) {
        return this.page(page);
    }

    @Override
    public User getCurrentById(Integer currentId) {
        User user = userMapper.selectCurrentById(currentId);

        log.info("当前用户：{}", user);
        return user;
    }

    @Override
    public Boolean updateUserStatusById(Integer id) {
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("user_id", id)
                .set("is_active", 1);
        return this.update(updateWrapper);
    }

    @Override
    public Page<User> searchUsers(Page<User> page, @Param("keyword") String keyword) {
        return userMapper.searchUsers(page, keyword);
    }


}
