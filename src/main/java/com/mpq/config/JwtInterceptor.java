package com.mpq.config;

import com.mpq.service.UserService;
import com.mpq.utils.JwtTokenUtil;
import com.mpq.utils.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true; // 放行预检请求
        }
        // 从请求头获取token
        final String tokenHeader = request.getHeader("Authorization");
//        log.info("tokenHeader: {}", tokenHeader);
        if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
            log.warn("请求未携带有效的Authorization Token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            log.info("未登录，请先登录");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录，请先登录\",\"data\":null}");
            return false;
        }

        String token = tokenHeader.substring(7); // 去掉"Bearer "前缀

        try {
            // 验证token有效性
            String username = jwtTokenUtil.extractUsername(token);
            if (username != null && jwtTokenUtil.validateToken(token, username)) {
                // 提取用户ID并存入ThreadLocal
                Integer userId = jwtTokenUtil.extractUserId(token);
                UserContextHolder.setUserId(userId);
                userService.addOnline(userId);

                // 从 Header 或 Cookie 获取 sessionId
                String sessionId = request.getHeader("X-Session-Id");
                if (sessionId == null || sessionId.isEmpty()) {
                    sessionId = (String) request.getAttribute("sessionId");
                }
                if (sessionId != null && !sessionId.isEmpty()) {
                    UserContextHolder.setSessionId(sessionId);
                }

                // 从 Header 获取 extra 和 actionValue
                String extra = request.getHeader("X-Extra");
                String actionValueStr = request.getHeader("X-Action-Value");
                if (extra != null && !extra.isEmpty()) {
                    UserContextHolder.setExtra(extra);
                }
                if (actionValueStr != null && !actionValueStr.isEmpty()) {
                    try {
                        UserContextHolder.setActionValue(Integer.parseInt(actionValueStr));
                    } catch (NumberFormatException e) {
                        log.debug("X-Action-Value 解析失败: {}", actionValueStr);
                    }
                }

                // 将用户名存入请求属性，供后续使用
                request.setAttribute("username", username);
                return true;
            }
        } catch (Exception e) {
            log.error("Token验证失败: {}", e.getMessage());
        }

        // 如果Token无效或过期，返回未登录提示
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\",\"data\":null}");
        return false;
    }


}
