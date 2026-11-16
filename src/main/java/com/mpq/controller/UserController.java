package com.mpq.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.common.Result;
import com.mpq.pojo.User;
import com.mpq.pojo.UserRole;
import com.mpq.pojo.dto.UpdatePasswordDto;
import com.mpq.service.EmailService;
import com.mpq.service.UserService;
import com.mpq.utils.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Slf4j
public class UserController {

    private final UserService userService;
    private final EmailService emailService;

    @GetMapping("/info")
    public Result getUserInfo() {
        Integer userId = Math.toIntExact(UserContextHolder.getUserId());

        User user = userService.getUserById(userId);
        if (user != null) {
            return Result.success("获取用户信息成功", user);
        }
        log.info("用户不存在");
        return Result.failure(404, "用户不存在");
    }

    @GetMapping("selectUserById/{userId}")
    public Result selectUserById(@PathVariable Integer userId) {
        User user = userService.getUserById(userId);
        if (user != null) {
            return Result.success("获取用户信息成功", user);
        }
        log.info("用户不存在");
        return Result.failure(404, "用户不存在");
    }


    @PostMapping("/update")
    public Result updateUserById(@RequestBody User user) {
        int userId = Math.toIntExact(UserContextHolder.getUserId());
        log.debug("更新用户信息，用户ID：{}", userId);
        log.debug("用户信息：{}", user);



        User userById = userService.getUserById(userId);
        if (!userById.getEmail().equals(user.getEmail())) {
            User userByEmail = userService.getUserByEmail(user.getEmail());
            if (userByEmail != null) {
                return Result.failure(400, "邮箱已存在");
            }
            if (user.getCode() == null || user.getCode().isEmpty()) {
                return Result.failure(400, "修改邮箱需要验证码");
            }
            boolean b = emailService.verifyCode(user.getEmail(), user.getCode());
            if (!b) {
                return Result.failure(400, "验证码错误");
            }
        }
        if (userById.getUserName().equals(user.getUserName())
                && userById.getEmail().equals(user.getEmail())
                && userById.getBio().equals(user.getBio())
                && userById.getAvatarUrl().equals(user.getAvatarUrl())) {
            return Result.failure(400, "用户信息未改变");
        }

        try {
            user.setUserId(userId);
            User userUpdated = userService.updateUserById(user);
            return Result.success("更新用户信息成功", userUpdated);
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return Result.failure(500, "更新用户信息失败");
        }
    }

    @PostMapping("/password/Update")
    public Result updatePassword(@RequestBody UpdatePasswordDto passwordDto) {
        int userId = Math.toIntExact(UserContextHolder.getUserId());
        try {
            passwordDto.setUserId(userId);
            Integer i = userService.updatePasswordById(passwordDto);
            if (i == 1) {
                return Result.success("更新用户密码成功");
            } else if (i == 2) {
                log.error("原密码错误");
                return Result.failure(400, "原密码错误");

            } else {
                log.error("更新用户密码失败");
                return Result.failure(500, "更新用户密码失败");
            }
        } catch (Exception e) {
            log.error("更新用户密码失败：", e);
            return Result.failure(500, "更新用户密码失败");
        }
    }

    @DeleteMapping("/delete")
    public Result deleteUserById(@RequestParam String password) {

        try {
            int userId = Math.toIntExact(UserContextHolder.getUserId());
//        密码校验
            if (!userService.verifyPassword(userId, password)) {
                return Result.failure(401, "密码验证失败");
            }
            log.debug("删除用户，用户ID：{}", userId);

            if (userService.deleteUserById(userId)) {
                return Result.success("删除用户成功");
            }
            return Result.failure(500, "删除用户失败");
        } catch (Exception e) {
            log.error("删除用户失败：", e);
            return Result.failure(500, "删除用户失败");
        }
    }

    @GetMapping("/getUserList")
    public Result getUserList(@RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int size) {
        log.debug("获取用户列表");
        Page<User> page = new Page<>(pageNum, size);
        Page<User> userPage = userService.getUserList(page);
        Map<String, Object> result = new HashMap<>();
        result.put("list", userPage.getRecords());
        result.put("total", userPage.getTotal());
        result.put("pages", userPage.getPages());
        result.put("current", userPage.getCurrent());
        result.put("size", userPage.getSize());
        return Result.success("获取用户列表成功", result);
    }

    //    管理员禁用
    @DeleteMapping("/deleteUserById/{id}")
    public Result deleteUserById(@PathVariable Integer id) {
        log.debug("删除用户，用户ID：{}", id);
        Integer currentId = UserContextHolder.getUserId();
        log.info("当前用户ID：{}", currentId);
        User currentUser = userService.getCurrentById(currentId);
        log.info("当前用户：{}", currentUser);
        if (!currentUser.getRole().equals(UserRole.admin)) {
            return Result.failure(401, "权限不足");
        }
        if (userService.deleteUserById(id)) {
            return Result.success("删除用户成功");
        }
        return Result.failure(500, "删除用户失败");
    }

    //    管理员启用
    @PutMapping("/updateUserIsActive")
    public Result updateUserIsActive(@RequestParam Integer id) {
        log.debug("更新用户状态，用户ID：{}", id);
        Integer currentId = UserContextHolder.getUserId();
        User currentUser = userService.getCurrentById(currentId);
        if (!currentUser.getRole().equals(UserRole.admin)) {
            return Result.failure(401, "权限不足");
        }
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.failure(404, "用户不存在");
        }
        if (userService.updateUserStatusById(id)) {
            return Result.success("更新用户状态成功");
        }
        return Result.failure(500, "更新用户状态失败");
    }

    @PostMapping("/adminUpdate")
    public Result AdminUpdateUserById(@RequestBody User user) {
        try {
            User userUpdated = userService.updateUserById(user);
            return Result.success("更新用户信息成功", userUpdated);
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return Result.failure(500, "更新用户信息失败");
        }
    }

    @GetMapping("/getAllUserNumber")
    public Result getAllUserNumber() {
        log.debug("获取用户数量");
        Long number = userService.count();
        return Result.success("获取用户数量成功", number);
    }

    @GetMapping("/{id}")
    public Result getUserById(@PathVariable Integer id) {
        log.debug("获取用户，用户ID：{}", id);
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.failure(404, "用户不存在");
        }
        return Result.success("获取用户成功", user);
    }

    @GetMapping("/search")
    public Result<Map<String, Object>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int size) {
        Page<User> page = new Page<>(pageNum, size);
        Page<User> result = userService.searchUsers(page, keyword);
        List<User> list = result.getRecords();
        Map<String, Object> map = new HashMap<>();
        map.put("list", list);
        map.put("total", result.getTotal());
        return Result.success("搜索成功", map);
    }

    /**
     * 发送验证码
     */
    @PostMapping("/sendCode")
    public Result sendCode(@RequestParam String email) {
        try {
            User userByEmail = userService.getUserByEmail(email);
            if (userByEmail != null) {
                return Result.failure(400, "邮箱已存在");
            }
            emailService.sendVerificationCode(email);
            return Result.success("验证码已发送至邮箱");
        } catch (Exception e) {
            log.error("发送验证码失败", e);
            return Result.failure(500, "发送验证码失败");
        }
    }
    @GetMapping("/getOnline")
    public Result sendCode(){
      long number =  userService.getUserOnline();
      return Result.success("获取成功",number);
    }
}
