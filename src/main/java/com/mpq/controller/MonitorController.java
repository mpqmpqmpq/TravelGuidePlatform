package com.mpq.controller;

import com.mpq.common.Result;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
@RestController
@RequestMapping("/monitor")
@CrossOrigin(origins = "*")
public class MonitorController {
    private static final Logger log = LoggerFactory.getLogger(MonitorController.class);

    @Autowired
    private MeterRegistry meterRegistry;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String PROMETHEUS_URL = "http://192.168.195.131:9090";
    private static final Set<Integer> onlineUsers = ConcurrentHashMap.newKeySet();
    @GetMapping("/metrics")
    public Result<Map<String, Object>> getMetrics() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("app", getAppMetrics());
            data.put("db", getDbMetrics());
            return Result.success("success", data);
        } catch (Exception e) {
            return Result.failure(500, e.getMessage());
        }
    }
    @GetMapping("/trend")
    public Result<Map<String, Object>> getTrend() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("requests", getRequestTrendFromPrometheus());
            data.put("responseTime", getResponseTimeTrendFromPrometheus());
            return Result.success("success", data);
        } catch (Exception e) {
            log.error("获取趋势数据失败", e);
            return Result.failure(500, e.getMessage());
        }
    }
//    取qps和错误率
    private Map<String, Object> getAppMetrics() {
        Map<String, Object> app = new HashMap<>();
        try {
            List<Timer> timers = new ArrayList<>();
//            当前所有注册的 Meter 列表
            for (Meter meter : meterRegistry.getMeters()) {
//                获取所有的Timer类型且包含http
                if (meter instanceof Timer && meter.getId().getName().contains("http")) {
                    timers.add((Timer) meter);
                }
            }
            //启动至今
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            long uptime = runtimeBean.getUptime() / 1000;

            long totalCount = 0;
            double totalTime = 0;

            for (Timer timer : timers) {
                totalCount += timer.count();
                totalTime += timer.totalTime(TimeUnit.SECONDS);
            }

            if (uptime > 0 && totalCount > 0) {
//                总条数/启动时间
                app.put("qps", Math.round(totalCount / (double) uptime * 10) / 10.0);
//                总耗时/总条数
                app.put("responseTime", Math.round(totalTime / totalCount * 1000));
            } else {
                app.put("qps", 0);
                app.put("responseTime", 0);
            }

            long errorCount = 0;
//            取5开头的错误请求
            for (Timer timer : timers) {
                String status = timer.getId().getTag("status");
                if (status != null && status.startsWith("5")) {
                    errorCount += timer.count();
                }
            }
//            错误数/总条数
            app.put("errorRate", totalCount > 0 ? Math.round((double) errorCount / totalCount * 100 * 100) / 100.0 : 0);
            app.put("activeUsers", onlineUsers.size());

        } catch (Exception e) {
            app.put("qps", 0);
            app.put("responseTime", 0);
            app.put("errorRate", 0);
            app.put("activeUsers", 0);
        }
        return app;
    }
    private Map<String, Object> getDbMetrics() {
        Map<String, Object> db = new HashMap<>();

        try {
            Double activeConn = null;
            Double maxConn = null;

            for (Meter meter : meterRegistry.getMeters()) {
                String name = meter.getId().getName();
                if (meter instanceof Gauge && name.contains("hikari")) {
                    if (name.contains("active") || name.contains("Active")) {
                        activeConn = ((Gauge) meter).value();
                    } else if (name.contains("max") || name.contains("Max")) {
                        maxConn = ((Gauge) meter).value();
                    }
                }
            }

            db.put("connections", activeConn != null ? activeConn.intValue() : 0);
            db.put("maxConnections", maxConn != null ? maxConn.intValue() : 50);

            db.put("queryTime", 0);
            db.put("slowQueries", 0);
            db.put("tps", 0);

        } catch (Exception e) {
            db.put("connections", 0);
            db.put("maxConnections", 50);
            db.put("queryTime", 0);
            db.put("slowQueries", 0);
            db.put("tps", 0);
        }

        return db;
    }
    private List<Long> getRequestTrendFromPrometheus() {
        List<Long> requests = new ArrayList<>();
        try {
            String query = "sum(increase(http_server_requests_seconds_count[5m]))";
            List<Double> values = queryPrometheusRange(query, 12);

            for (Double val : values) {
                requests.add(val != null ? val.longValue() : 0L);
            }
        } catch (Exception e) {
            log.error("获取请求趋势失败", e);
            for (int i = 0; i < 12; i++) {
                requests.add(0L);
            }
        }
        return requests;
    }
    private List<Long> getResponseTimeTrendFromPrometheus() {
        List<Long> responseTimes = new ArrayList<>();
        try {
//            rate计算开始和结束只差，除以时间
//            总耗时/总条数 = 平均响应时间
            String query = "sum(rate(http_server_requests_seconds_sum[5m])) / sum(rate(http_server_requests_seconds_count[5m]))";
            List<Double> values = queryPrometheusRange(query, 12);

            for (Double val : values) {
                responseTimes.add(val != null ? (long)(val * 1000) : 0L);
            }
        } catch (Exception e) {
            log.error("获取响应时间趋势失败", e);
            for (int i = 0; i < 12; i++) {
                responseTimes.add(0L);
            }
        }
        return responseTimes;
    }
    private List<Double> queryPrometheusRange(String query, int points) {
        List<Double> values = new ArrayList<>();
        try {
            long now = System.currentTimeMillis() / 1000;
//            1个小时前
            long oneHourAgo = now - 3600;
//            每五分钟去一次
            int step = 300;

            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            encodedQuery = encodedQuery.
                    replace("%28", "(").
                    replace("%29", ")").
                    replace("%2F", "/").
                    replace("%5B", "[").
                    replace("%5D", "]");

            String url = PROMETHEUS_URL + "/api/v1/query_range?query="
                    + encodedQuery
                    + "&start=" + oneHourAgo + "&end=" + now + "&step=" + step;

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && "success".equals(response.get("status"))) {
                Map<String, Object> dataMap = (Map<String, Object>) response.get("data");
                if (dataMap != null) {
                    List<?> results = (List<?>) dataMap.get("result");
                    if (results != null && !results.isEmpty()) {
                        Map<String, Object> firstResult = (Map<String, Object>) results.get(0);
                        List<?> valuesList = (List<?>) firstResult.get("values");

                        if (valuesList != null) {
//                            获取指标值，第一个是时间戳，第二个是指标值
                            for (Object v : valuesList) {
                                if (v instanceof List) {
                                    List<?> valuePair = (List<?>) v;
                                    if (valuePair.size() >= 2) {
                                        Object val = valuePair.get(1);
                                        try {
                                            values.add(Double.parseDouble(val.toString()));
                                        } catch (Exception e) {
                                            values.add(0.0);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            while (values.size() < points) {
                values.add(0.0);
            }
            if (values.size() > points) {
                values = values.subList(0, points);
            }

        } catch (Exception e) {
            log.error("查询Prometheus失败: {}", e.getMessage());
            for (int i = 0; i < points; i++) {
                values.add(0.0);
            }
        }
        return values;
    }
}