package com.mpq.config;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class SessionIdFilter implements Filter {

    private static final String SESSION_ID_COOKIE = "session_id";
    private static final int COOKIE_MAX_AGE = 24 * 60 * 60; // 24小时

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String sessionId = getSessionId(httpRequest);

        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            Cookie cookie = new Cookie(SESSION_ID_COOKIE, sessionId);
            cookie.setPath("/");
            cookie.setMaxAge(COOKIE_MAX_AGE);
            cookie.setHttpOnly(true);
            httpResponse.addCookie(cookie);
        }

        httpRequest.setAttribute("sessionId", sessionId);
        chain.doFilter(request, response);
    }

    private String getSessionId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_ID_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}