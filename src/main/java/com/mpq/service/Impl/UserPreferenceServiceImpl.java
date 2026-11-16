package com.mpq.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.mapper.UserPreferenceMapper;
import com.mpq.pojo.UserPreferences;
import com.mpq.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl extends ServiceImpl<UserPreferenceMapper, UserPreferences> implements UserPreferenceService {
    private final UserPreferenceMapper userPreferenceMapper;

    @Override
    public void deleteByUserId(Integer userId) {
        userPreferenceMapper.deleteByUserId(userId);
    }
}
