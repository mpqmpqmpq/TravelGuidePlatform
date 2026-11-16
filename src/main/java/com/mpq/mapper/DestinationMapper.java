package com.mpq.mapper;

import com.mpq.pojo.Destination;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 目的地信息表 Mapper 接口
 * </p>
 *
 * @author mpq
 * @since 2026-03-29
 */
@Mapper
public interface DestinationMapper extends BaseMapper<Destination> {

    boolean updateByDestinationId(Destination  destination);
}

