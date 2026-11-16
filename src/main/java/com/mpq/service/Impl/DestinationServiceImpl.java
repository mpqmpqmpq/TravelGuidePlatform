package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.common.Result;
import com.mpq.mapper.DestinationMapper;
import com.mpq.pojo.Destination;
import com.mpq.service.DestinationService;
import com.mpq.service.MinioService;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DestinationServiceImpl extends ServiceImpl<DestinationMapper, Destination> implements DestinationService {

    private final DestinationMapper destinationMapper;
    private final MinioService minioService;
    private static final String bucketName = "destination";

    @Override
    public Page<Destination> getList(Page<Destination> page, String province) {
        LambdaQueryWrapper<Destination> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Destination::getStatus, 1);
        if (StringUtils.hasText(province)) {
            wrapper.like(Destination::getProvince, province);
        }
        wrapper.orderByAsc(Destination::getRanking);
        return page(page, wrapper);
    }

    @Override
    public List<Destination> getHot(int size) {
        LambdaQueryWrapper<Destination> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Destination::getStatus, 1);
        wrapper.gt(Destination::getRanking, 0);
        wrapper.orderByAsc(Destination::getRanking);
        wrapper.last("LIMIT " + size);
        return list(wrapper);
    }

    @Override
    public Page<Destination> search(Page<Destination> page, String keyword) {
        LambdaQueryWrapper<Destination> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Destination::getStatus, 1);
        wrapper.and(w -> w.like(Destination::getName, keyword)
                .or().like(Destination::getDescription, keyword));
        return page(page, wrapper);
    }

    @Override
    public void incrementViewCount(Integer id) {
        Destination destination = getById(id);
        if (destination != null) {
            destination.setViewCount(destination.getViewCount() + 1);
            updateById(destination);
        }
    }

    @Override
    public Result addDestination(Destination destination) {
        LambdaQueryWrapper<Destination> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Destination::getName, destination.getName());
        if (count(wrapper) > 0) {
            return Result.failure(400, "目的地已存在");
        }
        boolean save = this.save(destination);
        if (save) {
            return Result.success("添加目的地成功");
        }
        return Result.failure(500, "添加目的地失败");
    }

    @Override
    public Result updateByDestinationId(Destination destination) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (destination.getId() == null) {
            return Result.failure(400, "目的地id不能为空");
        }
        Destination oldDestination = getById(destination.getId());
      boolean update = destinationMapper.updateByDestinationId(destination);
      if (update) {
          minioService.deleteImage(oldDestination.getCoverImage(), bucketName);
          return Result.success("更新成功");
      }
      minioService.deleteImage(destination.getCoverImage(), bucketName);
        return Result.failure(500, "更新失败");
    }

    @Override
    public Result updateViewById(Integer id) {
        Destination destination = getById(id);
        if (destination != null) {
            destination.setViewCount(destination.getViewCount()+1);
        }
        boolean b = destinationMapper.updateByDestinationId(destination);
        if (b) {
            return Result.success("更新成功");
        }
        return Result.failure(500, "更新失败");
    }

    @Override
    public Set<String> getProvince() {
        LambdaQueryWrapper<Destination> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Destination::getProvince);
        List<Destination> destinations = destinationMapper.selectList(wrapper);
        Set<String> provinces = destinations.stream().
                map(Destination::getProvince).
                collect(Collectors.toSet());
        return provinces;
    }
}