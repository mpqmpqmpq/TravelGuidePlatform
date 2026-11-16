package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.mapper.RouteMapper;
import com.mpq.mapper.RouteSpotMapper;
import com.mpq.pojo.Route;
import com.mpq.pojo.RouteSpot;
import com.mpq.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl extends ServiceImpl<RouteMapper, Route> implements RouteService {
    private final RouteSpotMapper routeSpotMapper;

    @Override
    public Page<Route> getList(Page<Route> page) {
        LambdaQueryWrapper<Route> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Route::getStatus, 1);
        wrapper.orderByDesc(Route::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public void incrementViewCount(Integer id) {
        Route route = getById(id);
        if (route != null) {
            route.setViewCount(route.getViewCount() + 1);
            updateById(route);
        }
    }

    @Override
    public Route create(Route route) {
        save(route);
        return route;
    }

    @Override
    public Route update(Route route) {
        updateById(route);
        return route;
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        // 删除路线关联的景点
        LambdaQueryWrapper<RouteSpot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RouteSpot::getRouteId, id);
        routeSpotMapper.delete(wrapper);
        // 删除路线
        removeById(id);
    }

    @Override
    public List<RouteSpot> getRouteSpots(Integer routeId) {
        LambdaQueryWrapper<RouteSpot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RouteSpot::getRouteId, routeId);
        wrapper.orderByAsc(RouteSpot::getDayNum);
        wrapper.orderByAsc(RouteSpot::getSpotOrder);
        return routeSpotMapper.selectList(wrapper);
    }

    @Override
    public void addSpot(RouteSpot spot) {
        routeSpotMapper.insert(spot);
    }

    @Override
    public void removeSpot(Integer routeId, Integer spotId) {
        LambdaQueryWrapper<RouteSpot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RouteSpot::getRouteId, routeId);
        wrapper.eq(RouteSpot::getSpotId, spotId);
        routeSpotMapper.delete(wrapper);
    }

    @Override
    public Page<Route> getByUserId(Page<Route> page, Integer userId) {
        LambdaQueryWrapper<Route> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Route::getUserId, userId);
        wrapper.orderByDesc(Route::getCreatedAt);
        return page(page, wrapper);
    }
}