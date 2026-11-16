package com.mpq.pojo.dto;

import lombok.Data;

@Data
public class UpdatePasswordDto {
    Integer UserId;
    String oldPassword;
    String newPassword;
}
