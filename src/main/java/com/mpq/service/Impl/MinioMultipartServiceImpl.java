package com.mpq.service.Impl;

import com.mpq.service.MinioMultipartService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * 基于 AWS S3 SDK 的 MinIO 分片上传实现
 * 统一使用 AWS 语法，避免 MinIO 类混用（优化：资源泄漏、分片号错误、内存溢出）
 */
@Service
@Slf4j

public class MinioMultipartServiceImpl implements MinioMultipartService {

    // 注入 AWS 官方 S3AsyncClient
    private final S3AsyncClient s3AsyncClient;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    private final ThreadPoolTaskExecutor videoUploadExecutor;

    public MinioMultipartServiceImpl(S3AsyncClient s3AsyncClient,
                                     @Qualifier("videoUploadExecutor") ThreadPoolTaskExecutor videoUploadExecutor) {
        this.s3AsyncClient = s3AsyncClient;
        this.videoUploadExecutor = videoUploadExecutor;
    }


    /**
     * 初始化分片上传, AWS S3
     */
    @Override
    public String initMultipartUpload(String bucketName, String objectName, String contentType) throws Exception {
        checkBucketExists(bucketName);

        // 1. 构建 S3 标准初始化请求
        CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(objectName) // S3 的 key 对应 MinIO 的 objectName
                .contentType(contentType)
                .build();

        try {
            // 2. 异步转同步获取 uploadId
            CreateMultipartUploadResponse response = s3AsyncClient.createMultipartUpload(request).get();
            log.info("初始化分片成功: 桶={}, 文件名={}, uploadId={}", bucketName, objectName, response.uploadId());
            return response.uploadId();
        } catch (ExecutionException e) {
            // 处理执行异常
            Throwable cause = e.getCause();
            log.error("初始化分片失败: 桶={}, 文件名={}, 原因={}", bucketName, objectName, cause.getMessage());
            // 确保只抛出 Exception 类型
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw new Exception("初始化分片失败: " + cause.getMessage(), cause);
            }
        } catch (InterruptedException e) {
            // 处理线程中断异常
            Thread.currentThread().interrupt();
            log.error("初始化分片时线程被中断: 桶={}, 文件名={}", bucketName, objectName);
            throw new Exception("初始化分片时线程被中断", e);
        }
    }

    /**
     * 上传单个分片
     */
    @Override
    public String uploadChunk(String bucketName, String objectName, String uploadId, int chunkIndex, MultipartFile chunkFile) throws Exception {
        int partNumber = chunkIndex + 1;
        long chunkSize = chunkFile.getSize(); // 直接获取分片大小，不加载字节数组

        // 1. 构建分片上传请求
        UploadPartRequest request = UploadPartRequest.builder()
                .bucket(bucketName)
                .key(objectName)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .contentLength(chunkSize) // 指定分片大小，避免 S3 校验失败
                .build();

        // 2. 流式上传：直接用 MultipartFile 输入流，避免 byte[] 加载导致 OOM
        try (InputStream inputStream = chunkFile.getInputStream()) {
            // 构建异步请求体
            AsyncRequestBody requestBody = AsyncRequestBody.fromInputStream(
                    inputStream,
                    chunkSize,
                    videoUploadExecutor.getThreadPoolExecutor()
            );

            // 3. 执行分片上传
            UploadPartResponse response = s3AsyncClient.uploadPart(request, requestBody).get();
            log.info("分片上传成功: 桶={}, 文件名={}, uploadId={}, 分片号={}, 大小={}B",
                    bucketName, objectName, uploadId, partNumber, chunkSize);
            return response.eTag();
        } catch (ExecutionException e) {
            // 处理执行异常
            Throwable cause = e.getCause();
            log.error("分片上传失败: 桶={}, 文件名={}, uploadId={}, 分片号={}, 原因={}",
                    bucketName, objectName, uploadId, partNumber, cause.getMessage());
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw new Exception("分片上传失败: " + cause.getMessage(), cause);
            }
        } catch (InterruptedException e) {
            // 处理线程中断
            Thread.currentThread().interrupt();
            log.error("分片上传时线程被中断: 桶={}, 文件名={}, uploadId={}, 分片号={}",
                    bucketName, objectName, uploadId, partNumber);
            throw new Exception("分片上传时线程被中断", e);
        }
    }

    /**
     * 合并所有分片
     */
    @Override
    public String completeMultipartUpload(String bucketName, String objectName, String uploadId, List<String> etagList) throws Exception {
        // 1. 转换 ETag 为 S3 要求的 CompletedPart 列表
        List<CompletedPart> completedParts = IntStream.range(0, etagList.size())
                .mapToObj(index -> {
                    String etag = etagList.get(index);
                    int partNumber = index + 1; // 索引+1 = 分片号
                    return CompletedPart.builder()
                            .partNumber(partNumber)
                            .eTag(etag)
                            .build();
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        // 2. 构建合并请求
        CompleteMultipartUploadRequest request = CompleteMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(objectName)
                .uploadId(uploadId)
                .multipartUpload(CompletedMultipartUpload.builder()
                        .parts(completedParts)
                        .build())
                .build();

        try {
            // 3. 执行合并
            s3AsyncClient.completeMultipartUpload(request).get();
            // 生成访问 URL
            String fileUrl = minioEndpoint + "/" + bucketName + "/" + objectName;
            log.info("分片合并成功: 桶={}, 文件名={}, uploadId={}, 访问URL={}",
                    bucketName, objectName, uploadId, fileUrl);
            return fileUrl;
        } catch (ExecutionException e) {
            // 处理执行异常
            Throwable cause = e.getCause();
            log.error("分片合并失败: 桶={}, 文件名={}, uploadId={}, 原因={}",
                    bucketName, objectName, uploadId, cause.getMessage());
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw new Exception("分片合并失败: " + cause.getMessage(), cause);
            }
        } catch (InterruptedException e) {
            // 处理线程中断
            Thread.currentThread().interrupt();
            log.error("分片合并时线程被中断: 桶={}, 文件名={}, uploadId={}",
                    bucketName, objectName, uploadId);
            throw new Exception("分片合并时线程被中断", e);
        }
    }

    /**
     * 取消分片上传
     */
    @Override
    public void abortMultipartUpload(String bucketName, String objectName, String uploadId) throws Exception {
        // 1. 构建取消请求
        AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(objectName)
                .uploadId(uploadId)
                .build();

        try {
            s3AsyncClient.abortMultipartUpload(request).get();
            log.info("取消分片成功: 桶={}, 文件名={}, uploadId={}", bucketName, objectName, uploadId);
        } catch (ExecutionException e) {
            // 处理执行异常（如网络错误、权限问题等）
            Throwable cause = e.getCause();
            log.error("取消分片失败: 桶={}, 文件名={}, uploadId={}, 原因={}",
                    bucketName, objectName, uploadId, cause.getMessage());
            // 转换为 Exception 抛出，保证异常类型合规
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw new Exception("取消分片失败: " + cause.getMessage(), cause);
            }
        } catch (InterruptedException e) {
            // 处理线程中断异常
            Thread.currentThread().interrupt();
            log.error("取消分片时线程被中断: 桶={}, 文件名={}, uploadId={}",
                    bucketName, objectName, uploadId);
            throw new Exception("取消分片时线程被中断", e);
        }
    }

    /**
     * 检查桶是否存在
     */
    private void checkBucketExists(String bucketName) throws Exception {
        try {
            // 1. S3 检查桶存在的标准方式：发送 HEAD 请求
            HeadBucketRequest headRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3AsyncClient.headBucket(headRequest).get(); // 无异常则桶存在
            log.debug("桶已存在: {}", bucketName);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            // 2. 捕获"桶不存在"异常，执行创建
            if (cause instanceof NoSuchBucketException) {
                CreateBucketRequest createRequest = CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .createBucketConfiguration(
                                CreateBucketConfiguration.builder()
                                        .locationConstraint("us-east-1") // MinIO 兼容默认区域
                                        .build()
                        )
                        .build();
                s3AsyncClient.createBucket(createRequest).get();
                log.info("桶不存在，已创建: {}", bucketName);
            } else {
                log.error("检查桶失败: 桶={}, 原因={}", bucketName, cause.getMessage());
                // 3. 确保只抛出 Exception 类型
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                } else {
                    throw new Exception("检查桶失败: " + cause.getMessage(), cause);
                }
            }
        } catch (InterruptedException e) {
            // 4. 处理线程中断异常
            Thread.currentThread().interrupt();
            log.error("检查桶时线程被中断: 桶={}", bucketName);
            throw new Exception("检查桶时线程被中断", e);
        }
    }

    /**
     * 应用关闭时销毁全局线程池：避免资源泄漏
     */
    @PreDestroy
    public void destroyExecutor() {
        log.info("开始销毁视频上传线程池");
        videoUploadExecutor.shutdown();
        try {
            if (!videoUploadExecutor.getThreadPoolExecutor().awaitTermination(30, TimeUnit.SECONDS)) {
                videoUploadExecutor.getThreadPoolExecutor().shutdownNow();
                log.warn("视频上传线程池超时未关闭，已强制终止");
            } else {
                log.info("视频上传线程池正常销毁");
            }
        } catch (InterruptedException e) {
            videoUploadExecutor.getThreadPoolExecutor().shutdownNow();
            Thread.currentThread().interrupt();
            log.error("销毁视频上传线程池时被中断", e);
        }
    }

}