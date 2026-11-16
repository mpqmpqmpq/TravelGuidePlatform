package com.mpq.service;

import io.minio.errors.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface MinioService {

    void deleteFile(String bucketName, String objectName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;


    String getFileNameFromObjectName(String objectName);
    /*
    * 上传图片
    * */
    String generateArticleMediaObjectName(String originalFileName, boolean isVideo);
    /*
    * 上传头像
    * */
    String uploadImage(byte[] fileBytes,String originalFileName, String contentType,String bucketName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;
    /*
    * 删除图片
    * */
    void  deleteImage(String fileUrl,String bucketName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;
    /*
    * 生成图片文件名
    * */
    String generateAvatarFileName(String originalFileName);
    /*
    * 获取文件访问URL
    * */
    String getFileUrl(String fileName,String bucketName);
    /*
    * 获取头像文件名
    * */
    String getImageFileName( String fileUrl,String bucketName);
    /*
     * 检查存储桶是否存在，不存在则创建
     */
    void checkBucketExists(String bucketName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;

    /*
//    * 上传文章图片
//    * */
//    String uploadArticleImages(byte[] fileBytes,String originalFileName, String contentType) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;

    // 统一上传媒体（支持图片/视频，适配article_media表）
    String uploadArticleMedia(byte[] fileBytes, String originalFileName,
                              String contentType, boolean isVideo) throws Exception;
    // 上传视频封面（复用图片逻辑）
    String uploadVideoCover(byte[] coverBytes, String originalFileName) throws Exception;

    String parseObjectNameFromUrl(String mediaUrl);

    InputStream getFileInputStream(String objectName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;

    String getFileExtension(String originalFileName);


}
