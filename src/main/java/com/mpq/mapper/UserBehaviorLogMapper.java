package com.mpq.mapper;

import com.mpq.pojo.UserBehaviorLog;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 用户行为原始日志表 Mapper 接口
 * </p>
 *
 * @author mpq
 * @since 2026-05-05
 */
@Mapper
public interface UserBehaviorLogMapper extends BaseMapper<UserBehaviorLog> {

}
