package com.mpq.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.pojo.Route;
import com.mpq.pojo.RouteSpot;
import java.util.List;
public interface RouteService extends IService<Route> {
    Page<Route> getList(Page<Route> page);
    void incrementViewCount(Integer id);
    Route create(Route route);
    Route update(Route route);
    void delete(Integer id);
    List<RouteSpot> getRouteSpots(Integer routeId);
    void addSpot(RouteSpot spot);
    void removeSpot(Integer routeId, Integer spotId);

    Page<Route> getByUserId(Page<Route> page, Integer userId);
}