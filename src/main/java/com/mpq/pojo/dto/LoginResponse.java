package com.mpq.pojo.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private String username;
    private Integer userId;
    private String avatarUrl;
    private String role ;
}