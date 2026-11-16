package com.mpq.utils;

import com.alibaba.dashscope.app.ApplicationParam;

import java.util.Collections;
import java.util.List;

public class IntelligentAuditingUtil {
    public static ApplicationParam getApplicationParam(String applicationId, String content) {

        return ApplicationParam.builder()
                .appId(applicationId)
                .apiKey(System.getenv("DASH_SCOPE_API_KEY"))
                .prompt(content)
                .topP(0.8)  // 提高创造性
                .temperature(0.3f)  // 降低随机性
                .build();
    }

    public static ApplicationParam ImagesAudit(String applicationId, List<String> imageUrList) {

        return ApplicationParam.builder()
                .appId(applicationId)
                .apiKey(System.getenv("DASH_SCOPE_API_KEY"))
                .prompt("图片审核")
                .images(List.of(imageUrList.get(0)))
                .topP(0.8)  // 提高创造性
                .temperature(0.3f)  // 降低随机性
                .build();

    }
}
