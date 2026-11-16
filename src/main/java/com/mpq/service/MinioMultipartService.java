package com.mpq.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * MinIO 分片上传专用接口
 */
public interface MinioMultipartService {

    /**
     * 初始化分片上传
     * @param bucketName 桶名
     * @param objectName 目标文件名
     * @param contentType 文件类型
     * @return 分片上传标识 uploadId
     */
    String initMultipartUpload(String bucketName, String objectName, String contentType) throws Exception;

    /**
     * 上传单个分片
     * @param bucketName 桶名
     * @param objectName 目标文件名
     * @param uploadId 分片上传标识
     * @param chunkIndex 分片索引（从 0 开始）
     * @param chunkFile 分片文件
     * @return 分片 ETag（用于合并）
     */
    String uploadChunk(String bucketName, String objectName, String uploadId, int chunkIndex, MultipartFile chunkFile) throws Exception;

    /**
     * 合并所有分片
     * @param bucketName 桶名
     * @param objectName 目标文件名
     * @param uploadId 分片上传标识
     * @param etagList 所有分片的 ETag 列表
     * @return 合并后的文件访问 URL
     */
    String completeMultipartUpload(String bucketName, String objectName, String uploadId, List<String> etagList) throws Exception;

    /**
     * 取消分片上传（可选，用于异常场景清理）
     * @param bucketName 桶名
     * @param objectName 目标文件名
     * @param uploadId 分片上传标识
     */
    void abortMultipartUpload(String bucketName, String objectName, String uploadId) throws Exception;
}