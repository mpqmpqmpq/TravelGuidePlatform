package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

// 新增角色枚举
public enum UserRole {
    admin("admin"),
    user("user");

    @EnumValue
    @JsonValue
    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}