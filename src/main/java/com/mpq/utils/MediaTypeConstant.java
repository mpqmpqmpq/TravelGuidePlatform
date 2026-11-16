package com.mpq.utils;

/**
 * 媒体类型常量类（替代枚举，适配String类型的mediaType字段）
 */
public class MediaTypeConstant {
    // 对应article_media表的media_type字段
    public static final String IMAGE = "image"; // 图片类型
    public static final String VIDEO = "video"; // 视频类型

    // 校验是否为合法媒体类型
    public static boolean isValidType(String mediaType) {
        return IMAGE.equals(mediaType) || VIDEO.equals(mediaType);
    }

    // 根据文件ContentType判断媒体类型
    public static String getTypeByContentType(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("文件类型不能为空");
        }
        if (contentType.startsWith("video/")) {
            return VIDEO;
        } else if (contentType.startsWith("image/")) {
            return IMAGE;
        } else {
            throw new IllegalArgumentException("不支持的文件类型：" + contentType);
        }
    }
}