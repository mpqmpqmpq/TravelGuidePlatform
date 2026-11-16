package com.mpq.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 路线景点关联表
 * </p>
 *
 * @author mpq
 * @since 2026-03-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("route_spot")
public class RouteSpot implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 路线ID
     */
    @TableField("route_id")
    private Integer routeId;

    /**
     * 景点ID
     */
    @TableField("spot_id")
    private Integer spotId;

    /**
     * 目的地ID
     */
    @TableField("destination_id")
    private Integer destinationId;

    /**
     * 第几天
     */
    @TableField("day_num")
    private Integer dayNum;

    /**
     * 顺序
     */
    @TableField("spot_order")
    private Integer spotOrder;





}
