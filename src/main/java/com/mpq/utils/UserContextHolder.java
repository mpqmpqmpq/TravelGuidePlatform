package com.mpq.utils;

public class UserContextHolder {
    private static final ThreadLocal<Integer> currentUserId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentSessionId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentExtra = new ThreadLocal<>();
    private static final ThreadLocal<Integer> currentActionValue = new ThreadLocal<>();

    public static void setUserId(Integer userId) {
        currentUserId.set(userId);
    }

    public static Integer getUserId() {
        return currentUserId.get();
    }

    public static void setSessionId(String sessionId) {
        currentSessionId.set(sessionId);
    }

    public static String getSessionId() {
        return currentSessionId.get();
    }

    public static void setExtra(String extra) {
        currentExtra.set(extra);
    }

    public static String getExtra() {
        return currentExtra.get();
    }

    public static void setActionValue(Integer actionValue) {
        currentActionValue.set(actionValue);
    }

    public static Integer getActionValue() {
        return currentActionValue.get();
    }

    public static void clear() {
        currentUserId.remove();
        currentSessionId.remove();
        currentExtra.remove();
        currentActionValue.remove();
    }
}