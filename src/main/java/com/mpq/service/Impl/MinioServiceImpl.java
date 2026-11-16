package com.mpq.service.Impl;


import com.mpq.service.MinioService;
import io.minio.*;

import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioServiceImpl implements MinioService {
    private final MinioClient minioClient;

//    @Value("${minio.bucket-name}")
//    private String bucketName;

    private static final String ARTICLE_MEDIA_BUCKET = "article-media";

    @Value("${minio.endpoint}")
    private String endpoint;


    /**
     * 删除MinIO中的文件
     */
    @Override
    public void deleteFile(String bucketName, String objectName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("MinIO文件删除成功，bucket: {}, object: {}", bucketName, objectName);
        } catch (ServerException | InsufficientDataException | ErrorResponseException
                 | IOException | NoSuchAlgorithmException | InvalidKeyException
                 | InvalidResponseException | XmlParserException | InternalException e) {
            log.error("删除MinIO文件失败，bucket: {}, object: {}", bucketName, objectName, e);
            throw e; // 抛出异常，让控制器层统一处理
        }
    }

    /**
     * 获取文件访问URL
     *
     * @return 文件访问URL
     */
    @Override
    public String getFileNameFromObjectName(String objectName) {
        if (objectName == null || objectName.isEmpty()) {
            throw new IllegalArgumentException("objectName 不能为空");
        }
        // 截取最后一个 "/" 后的内容，即剥离路径的纯文件名
        int lastSlashIndex = objectName.lastIndexOf("/");
        return lastSlashIndex == -1 ? objectName : objectName.substring(lastSlashIndex + 1);
    }

    /**
     * 统一生成文章媒体的 objectName
     * 规则：article/media/[image/video]/[UUID].[后缀]
     *
     * @param originalFileName 原始文件名
     * @param isVideo          是否为视频
     * @return 标准 objectName
     */
    @Override
    public String generateArticleMediaObjectName(String originalFileName, boolean isVideo) {
        String pathPrefix = isVideo ? "video/" : "image/";
        String extension = getFileExtension(originalFileName);
        // 复用 UUID 生成唯一文件名，避免重复
        return "article/media/" + pathPrefix + UUID.randomUUID() + extension;
    }


    /**
     * 上传图片
     *
     * @param fileBytes       文件字节数组
     * @param originalFileName 原始文件名
     * @param contentType     文件类型
     * @param bucketName      存储桶名称
     * @return 文件访问URL
     */
    @Override
    public String uploadImage(byte[] fileBytes, String originalFileName, String contentType, String bucketName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        checkBucketExists(bucketName);
        String fileName;
        if (bucketName.equals("user-avatar")) {
            fileName = generateAvatarFileName(originalFileName);
        } else {
            fileName = generateArticleFileName(originalFileName);
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(bis, fileBytes.length, -1)
                            .contentType(contentType)
                            .build()
            );

        } catch (IOException | ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            throw new RuntimeException(e);
        }
        log.info("url:{}", getFileUrl(fileName, bucketName));
        return getFileUrl(fileName, bucketName);
    }
    /**
     * 删除图片
     *
     * @param fileUrl     文件访问URL
     * @param bucketName  存储桶名称
     */
    @Override
    public void deleteImage(String fileUrl, String bucketName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String fileName = getImageFileName(fileUrl, bucketName);
        if (fileName != null) {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
        }
    }

    /**
     * 生成头像文件名
     *
     * @param originalFileName 原始文件名
     * @return 头像文件名
     */
    @Override
    public String generateAvatarFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uuid = UUID.randomUUID().toString();
        return "avatar/" + uuid + extension;
    }

    /**
     * 统一生成文章文件名
     *
     * @param originalFileName 原始文件名
     * @return 标准文件名
     */
    public String generateArticleFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uuid = UUID.randomUUID().toString();
        return "article/" + uuid + extension;
    }

    /**
     * 获取文件访问URL
     *
     * @param fileName   文件名
     * @param bucketName 存储桶名称
     * @return 文件访问URL
     */
    @Override
    public String getFileUrl(String fileName, String bucketName) {
        try {
            // URL编码处理特殊字符
            String encodedFileName = URLEncoder.encode(fileName, "UTF-8");
            // 移除endpoint末尾斜杠
            String baseUrl = endpoint.replaceAll("/+$", "");
            // 拼接永久有效的公开URL
            return baseUrl + "/" + bucketName + "/" + encodedFileName;
        } catch (Exception e) {
            log.error("获取文件URL异常", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取图片文件名
     * @param fileUrl
     * @param bucketName
     * @return
     */
    @Override
    public String getImageFileName(String fileUrl, String bucketName) {
        if (fileUrl == null || !fileUrl.contains(bucketName)) {
            return null;
        }
        return fileUrl.substring(fileUrl.indexOf(bucketName) + bucketName.length() + 1);
    }

    /**
     * 上传文章媒体文件（图片或视频）
     */
    @Override
    public String uploadArticleMedia(byte[] fileBytes, String originalFileName,
                                     String contentType, boolean isVideo) throws Exception {
        // 1. 检查媒体桶是否存在，不存在则创建
        checkBucketExists(ARTICLE_MEDIA_BUCKET);
        // 2. 调用统一方法生成 objectName
        String fileName = generateArticleMediaObjectName(originalFileName, isVideo);
        // 3. 上传到MinIO
        try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(ARTICLE_MEDIA_BUCKET)
                            .object(fileName)
                            .stream(bis, fileBytes.length, -1)
                            .contentType(contentType) // 视频传video/mp4，图片传image/jpeg
                            .build()
            );
        }
        // 4. 返回访问URL（逻辑不变）
        String mediaUrl = endpoint + "/" + ARTICLE_MEDIA_BUCKET + "/" + fileName;
        log.info("文章媒体上传成功：类型={}, URL={}, objectName={}", isVideo ? "video" : "image", mediaUrl, fileName);
        return mediaUrl;
    }

    /**
     * 上传视频封面
     */
    @Override
    public String uploadVideoCover(byte[] coverBytes, String originalFileName) throws Exception {
        return uploadImage(
                coverBytes,
                originalFileName,
                "image/jpeg", // 封面默认JPG格式
                ARTICLE_MEDIA_BUCKET // 存入媒体专用桶
        );
    }

    /**
     * 解析媒体URL得到objectName
     */
    @Override
    public String parseObjectNameFromUrl(String mediaUrl) {
        if (mediaUrl == null || !mediaUrl.contains(ARTICLE_MEDIA_BUCKET)) {
            return null;
        }

        // 1. 截取桶名（article-media）后的部分（包含路径和可能的参数）
        String objectName = extractObjectName(mediaUrl);

        if (objectName.isEmpty()) {
            throw new IllegalArgumentException("无法从URL中解析出objectName: " + mediaUrl);
        }

        log.info("解析封面URL得到objectName: {}", objectName); // 日志验证
        return objectName;
    }


    /**
     * 从URL中提取objectName
     */
    @NotNull
    private static String extractObjectName(String mediaUrl) {
        int bucketIndex = mediaUrl.indexOf(ARTICLE_MEDIA_BUCKET);
        String afterBucket = mediaUrl.substring(bucketIndex + ARTICLE_MEDIA_BUCKET.length());

        // 2. 移除URL中的签名参数（从?开始的部分）
        int paramStart = afterBucket.indexOf('?');
        if (paramStart != -1) {
            afterBucket = afterBucket.substring(0, paramStart);
        }

        // 3. 移除开头的分隔符（/），得到纯净的objectName
        String objectName = afterBucket.replaceFirst("^/", "");
        return objectName;
    }

   /**
     * 获取文件输入流
     */
    @Override
    public InputStream getFileInputStream(String objectName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(ARTICLE_MEDIA_BUCKET)
                        .object(objectName)
                        .build()
        );
    }


    // 提取文件后缀（兼容.mp4、.jpg等）
    public String getFileExtension(String originalFileName) {
        if (originalFileName == null || !originalFileName.contains(".")) {
            return "";
        }
        return originalFileName.substring(originalFileName.lastIndexOf("."));
    }



    /**
     * 检查存储桶是否存在，不存在则创建
     */
    @Override
    public void checkBucketExists(String bucketName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {

        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!found) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
            );
        }
    }
}
