package com.mpq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.pojo.UserPreferences;

public interface UserPreferenceService extends IService<UserPreferences> {
    void deleteByUserId(Integer userId);
}
