package com.mpq.controller;

import com.mpq.common.Result;

import com.mpq.pojo.dto.EmailLoginDto;
import com.mpq.pojo.dto.LoginDto;
import com.mpq.pojo.dto.LoginResponse;
import com.mpq.pojo.User;
import com.mpq.service.EmailService;
import com.mpq.service.UserService;

import com.mpq.utils.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtTokenUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    @PostMapping("/login")
    public Result login(@RequestBody LoginDto loginDto) {
        String userName = loginDto.getUserName();
        String password = loginDto.getPassword();
        String captcha = loginDto.getCaptcha();
        String captchaKey = loginDto.getCaptchaKey();
        // 参数验证
        if (userName == null || userName.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            return Result.failure(400, "用户名和密码不能为空");
        }
            String storedCaptcha = redisTemplate.opsForValue().get(captchaKey);
            if (storedCaptcha == null) {
                return Result.failure(401, "验证码已过期，请重新生成");
            }
            if (!storedCaptcha.equalsIgnoreCase(captcha)) {
                return Result.failure(400, "验证码错误");
            }
        // 用户认证
        User user = userService.login(userName, password);
        if (user == null) {
            return Result.failure(401, "用户名或密码错误");
        }
//        //删除验证码
        redisTemplate.delete(captchaKey);
        // 生成JWT令牌
        String token = jwtUtil.generateToken(userName, user.getUserId(), 
                user.getRole().getValue());
        // 构建响应1
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUsername(userName);
        response.setUserId(user.getUserId());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setRole(user.getRole().getValue());
        userService.addOnline(user.getUserId());
        return Result.success("登录成功", response);
    }
    @PostMapping("/email/sendCode")
    public Result sendEmailCode(@RequestParam String email) {
        if (email == null || email.trim().isEmpty()) {
            return Result.failure(400, "邮箱不能为空");
        }
        try {
            emailService.sendVerificationCode(email.trim());
            return Result.success("验证码已发送");
        } catch (Exception e) {
            log.error("发送邮箱验证码失败", e);
            return Result.failure(500, "发送验证码失败，请稍后重试");
        }
    }
    @PostMapping("/email/login")
    public Result emailLogin(@RequestBody EmailLoginDto emailLoginDto) {
        String email = emailLoginDto.getEmail();
        String code = emailLoginDto.getCode();

        if (email == null || email.trim().isEmpty()) {
            return Result.failure(400, "邮箱不能为空");
        }
        if (code == null || code.trim().isEmpty()) {
            return Result.failure(400, "验证码不能为空");
        }

        boolean isValid = emailService.verifyCode(email.trim(), code);
        if (!isValid) {
            return Result.failure(401, "验证码错误或已过期");
        }

        User user = userService.getUserByEmail(email.trim());
        if (user == null) {
            return Result.failure(404, "该邮箱未注册");
        }

        String token = jwtUtil.generateToken(user.getUserName(), user.getUserId(), user.getRole().getValue());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUsername(user.getUserName());
        response.setUserId(user.getUserId());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setRole(user.getRole().getValue());
        userService.addOnline(user.getUserId());
        return Result.success("登录成功", response);
    }

    @PostMapping("/regenerateCaptcha")
    public Result regenerateCaptcha() {
        String captchaKey = "captcha:" + UUID.randomUUID().toString(); // 唯一标识
        String captchaCode = generateRandomCaptcha(6); // 生成随机验证码

        // 存储验证码到 Redis，并设置过期时间为5分钟
        redisTemplate.opsForValue().set(captchaKey, captchaCode, 5, TimeUnit.MINUTES);

        log.info("重新生成验证码: {}，key: {}", captchaCode, captchaKey);
        return Result.success("验证码已重新生成", captchaKey);
    }

    private String generateRandomCaptcha(int length) {
        String chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return sb.toString();
    }

    @GetMapping("/captcha")
    public ResponseEntity<byte[]> getCaptcha(@RequestParam String key) {
        // 从Redis获取验证码
        String code = redisTemplate.opsForValue().get(key);
        if (code == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // 创建图片
            int width = 150;
            int height = 38;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            // 设置背景色
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);

            Random random = new Random();
            // 添加干扰线
            g.setColor(Color.LIGHT_GRAY);
            for (int i = 0; i < 8; i++) {
                int x1 = random.nextInt(width);
                int y1 = random.nextInt(height);
                int x2 = random.nextInt(width);
                int y2 = random.nextInt(height);
                g.drawLine(x1, y1, x2, y2);
            }

            // 设置字体
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.setColor(Color.BLUE);

            // 在不同位置绘制验证码字符
            for (int i = 0; i < code.length(); i++) {
                g.setColor(new Color(random.nextInt(255), random.nextInt(255), random.nextInt(255)));
                // 添加随机旋转
                double degree = random.nextDouble() * 0.3 - 0.15;
                g.rotate(degree, 15 + i * 20, height / 2);
                g.drawString(String.valueOf(code.charAt(i)), 15 + i * 20, 28);
                g.rotate(-degree, 15 + i * 20, height / 2);
            }

            g.dispose();
            // 将图片转换为字节数组
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setCacheControl("no-cache");
            headers.setPragma("no-cache");

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(imageBytes);

        } catch (Exception e) {
            log.error("生成验证码图片失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/userLogout")
    public Result logout(@RequestHeader("Authorization") String token){
        Integer userId = jwtUtil.extractUserId(token);
        userService.removeOnline(userId);
        return Result.success("登出成功");
    }

}