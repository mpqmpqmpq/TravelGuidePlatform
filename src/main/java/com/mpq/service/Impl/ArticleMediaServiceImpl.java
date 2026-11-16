package com.mpq.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mpq.mapper.ArticleMediaMapper;
import com.mpq.pojo.ArticleMedia;
import com.mpq.pojo.dto.ArticleMediaAddDto;
import com.mpq.pojo.dto.MediaChunkCompleteDto;
import com.mpq.service.ArticleMediaService;
import com.mpq.service.MinioMultipartService;
import com.mpq.service.MinioService;
import com.mpq.utils.MediaTypeConstant;
import io.minio.errors.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ArticleMediaServiceImpl extends ServiceImpl<ArticleMediaMapper, ArticleMedia> implements ArticleMediaService {
    private final ArticleMediaMapper articleMediaMapper;
    private final MinioService minioService;
    private final MinioMultipartService minioMultipartService;

    private final  static  String  ARTICLE_MEDIA_BUCKET = "article-media";
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ArticleMedia addArticleMedia(ArticleMediaAddDto articleMediaAddDto, Integer userId) throws Exception {
        // 参数校验
        if (articleMediaAddDto.getArticleId() == null
                || articleMediaAddDto.getMediaFile() == null
                || articleMediaAddDto.getMediaFile().isEmpty()) {
            throw new IllegalArgumentException("文章ID和媒体文件不能为空");
        }

        MultipartFile mediaFile = articleMediaAddDto.getMediaFile();

        String originalFilename = mediaFile.getOriginalFilename();

        String contentType = mediaFile.getContentType();
        contentType = contentType == null ? "application/octet-stream" : contentType;

        if ("application/octet-stream".equals(contentType)) {
            if (originalFilename == null || !originalFilename.contains(".")) {
                throw new IllegalArgumentException("无法识别文件类型，请上传图片或视频");
            }
            // 提取文件后缀（如 ".jpg"、".mp4"）
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            // 根据后缀匹配类型
            switch (extension) {
                case ".jpg":
                case ".jpeg":
                case ".png":
                case ".gif":
                    contentType = "image/jpeg"; // 图片统一按jpeg处理
                    break;
                case ".mp4":
                case ".avi":
                case ".mov":
                case ".webm":
                    contentType = "video/mp4"; // 视频统一按mp4处理
                    break;
                default:
                    throw new IllegalArgumentException("不支持的文件类型：" + extension);
            }
        }

//        校验文件类型
        String mediaType  = MediaTypeConstant.getTypeByContentType(contentType);
        boolean isVideo = MediaTypeConstant.VIDEO.equals(mediaType);

        byte[] fileBytes = mediaFile.getBytes() ;
        String mediaUrl = minioService.uploadArticleMedia(
                fileBytes,
                mediaFile.getOriginalFilename(),
                contentType,
                isVideo
        );
        String videoUrl = null;
        Integer videoDuration  = 0;
        if (mediaType.equals(MediaTypeConstant.VIDEO)){
            VideoProcessResult videoProcessResult = getVideoFirstFrameAndVideoDuration(mediaFile);
            byte[] coverBytes = videoProcessResult.coverBytes;
            videoUrl = minioService.uploadVideoCover(coverBytes,
                    mediaFile.getOriginalFilename()+"_cover");
            videoDuration = videoProcessResult.getDuration();
        }
        Integer targetPos =articleMediaAddDto.getPosition() == null ? 0 : articleMediaAddDto.getPosition();
        adjustMediaPosition(articleMediaAddDto.getArticleId(), targetPos);
        ArticleMedia articleMedia = new ArticleMedia();
        articleMedia.setArticleId(articleMediaAddDto.getArticleId());
        articleMedia.setMediaUrl(mediaUrl);
        articleMedia.setMediaType(mediaType);
        articleMedia.setPosition(targetPos);
        articleMedia.setUploadUserId(userId);
        articleMedia.setMediaTitle(articleMediaAddDto.getMediaTitle());
        articleMedia.setExtraInfo(articleMediaAddDto.getExtraInfo());
        articleMedia.setVideoDuration(videoDuration);
        articleMedia.setVideoCoverUrl(videoUrl);
        articleMedia.setStatus(1);
        articleMedia.setCreatedAt(LocalDateTime.now());
        articleMediaMapper.insert(articleMedia);

        return articleMedia;
    }

    /**
     * 根据文章ID查询所有媒体（支持按类型筛选）
     *
     * @param articleId
     * @param mediaType
     */
    @Override
    public List<ArticleMedia> getMediaListByArticleId(Integer articleId, String mediaType) {
        // 参数校验
        if (articleId == null) {
            throw new IllegalArgumentException("文章ID不能为空");
        }

        // 构建查询条件
        LambdaQueryWrapper<ArticleMedia> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ArticleMedia::getArticleId, articleId)
                .eq(ArticleMedia::getStatus, 1) // 只查询状态正常的媒体
                .orderByAsc(ArticleMedia::getPosition); // 按位置升序，确保展示顺序正确

        if (mediaType != null) {
            if (!MediaTypeConstant.IMAGE.equals(mediaType) && !MediaTypeConstant.VIDEO.equals(mediaType)) {
                throw new IllegalArgumentException("不支持的媒体类型：" + mediaType);
            }
            queryWrapper.eq(ArticleMedia::getMediaType, mediaType);
        }

        return articleMediaMapper.selectList(queryWrapper);
    }

    @Override
    public ArticleMedia getMediaById(Integer mediaId) {
        if (mediaId == null) {
            throw new IllegalArgumentException("媒体ID不能为空");
        }
        return articleMediaMapper.selectById(mediaId);
    }

    /**
     * 分片上传合并并创建媒体
     *
     * @param completeDto
     * @param userId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArticleMedia completeChunkUpload(MediaChunkCompleteDto completeDto, Integer userId) {

        try {
            // 1. 参数校验
            if (completeDto.getArticleId() == null || completeDto.getUploadId() == null
                    || completeDto.getFileName() == null || completeDto.getEtagList() == null
                    || completeDto.getContentType() == null) {
                throw new IllegalArgumentException("文章ID、uploadId、文件名、ETag列表和文件类型不能为空");
            }

            // 2. 直接使用前端传递的 fileName
            String objectName = completeDto.getFileName();
            String contentType = completeDto.getContentType();
            boolean isVideo = MediaTypeConstant.VIDEO.equals(MediaTypeConstant.getTypeByContentType(contentType));

            // 3. 合并分片
            String mediaUrl = minioMultipartService.completeMultipartUpload(
                    ARTICLE_MEDIA_BUCKET,
                    objectName,
                    completeDto.getUploadId(),
                    completeDto.getEtagList()
            );

            // 4. 视频封面处理
            String videoCoverUrl = null;
            Integer videoDuration = 0;
            if (isVideo) {
                try (InputStream videoStream = minioService.getFileInputStream(objectName)) {
                    VideoProcessResult result = getVideoFirstFrameAndVideoDuration(videoStream);
                    // 优化：用 MinioService 提取文件名，避免路径字符
                    String originalFileName = minioService.getFileNameFromObjectName(objectName);
                    videoCoverUrl = minioService.uploadVideoCover(
                            result.getCoverBytes(),
                            originalFileName + "_cover"
                    );
                    videoDuration = result.getDuration();
                }
            }

            // 5. 位置调整 +
            // 6. 保存记录
            Integer targetPos = completeDto.getPosition() == null ? 0 : completeDto.getPosition();
            adjustMediaPosition(completeDto.getArticleId(), targetPos);

            ArticleMedia articleMedia = new ArticleMedia();
            articleMedia.setArticleId(completeDto.getArticleId());
            articleMedia.setMediaUrl(mediaUrl);
            articleMedia.setMediaType(MediaTypeConstant.getTypeByContentType(contentType));
            articleMedia.setPosition(targetPos);
            articleMedia.setUploadUserId(userId);
            articleMedia.setMediaTitle(completeDto.getMediaTitle());
            articleMedia.setExtraInfo(completeDto.getExtraInfo());
            articleMedia.setVideoDuration(videoDuration);
            articleMedia.setVideoCoverUrl(videoCoverUrl);
            articleMedia.setStatus(1);
            articleMedia.setCreatedAt(LocalDateTime.now());
            articleMediaMapper.insert(articleMedia);

            return articleMedia;

        } catch (Exception e) {
            log.error("分片上传合并失败，articleId: {}, uploadId: {}, ",
                    completeDto.getArticleId(), completeDto.getUploadId(), e);
            throw new RuntimeException("分片上传合并失败：" + e.getMessage());
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByArticleId(Integer articleId )throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        // 1. 查询该文章下的所有媒体
        List<ArticleMedia> mediaList = list(new LambdaQueryWrapper<ArticleMedia>()
                .eq(ArticleMedia::getArticleId, articleId));
        if (mediaList.isEmpty()) return;

        // 2. 逐个删除MinIO文件和数据库记录
        for (ArticleMedia media : mediaList) {
            // 删除主文件（图片/视频）
            String objectName = minioService.parseObjectNameFromUrl(media.getMediaUrl());
            if (objectName != null && !objectName.isEmpty()) {
                minioService.deleteFile(ARTICLE_MEDIA_BUCKET, objectName);
            }

            // 若为视频，额外删除封面
            if ("video".equals(media.getMediaType()) && media.getVideoCoverUrl() != null) {
                String coverObjectName = minioService.parseObjectNameFromUrl(media.getVideoCoverUrl());
                if (coverObjectName != null && !coverObjectName.isEmpty()) {
                    minioService.deleteFile(ARTICLE_MEDIA_BUCKET, coverObjectName);
                }
            }

            // 删除数据库记录
            removeById(media.getMediaId());
        }
    }


    /**
     * 调整同文章内媒体位置（目标位置及以后的媒体+1）
     */
    private void adjustMediaPosition(Integer articleId, Integer targetPos) {
        LambdaUpdateWrapper<ArticleMedia> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ArticleMedia::getArticleId, articleId)
                .ge(ArticleMedia::getPosition, targetPos)
                .setSql("position = position + 1"); // 批量更新位置
        this.update(updateWrapper);
    }

    /**
     * 实现视频第一帧截取和获取视频时长
     */
    private VideoProcessResult getVideoFirstFrameAndVideoDuration(InputStream inputStream) throws Exception {
        try (
                // 直接使用 InputStream 初始化 FFmpeg 抓取器
                FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputStream);
                Java2DFrameConverter converter = new Java2DFrameConverter()
        ) {
            grabber.start(); // 启动抓取器
            Frame frame = grabber.grabImage(); // 抓取视频首帧

            if (frame == null) {
                throw new RuntimeException("无法截取视频封面，视频文件可能损坏或格式不支持");
            }

            // 转换帧为 JPG 字节数组
            BufferedImage image = converter.getBufferedImage(frame);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", outputStream);

            // 计算视频时长（毫秒转秒，向上取整）
            long lengthInTime = grabber.getLengthInTime();
            int duration = (int) Math.ceil(lengthInTime / 1000000.0);

            return new VideoProcessResult(outputStream.toByteArray(), duration);
        }
    }

    /**
     * 实现视频第一帧截取和获取视频时长
     */
    private VideoProcessResult getVideoFirstFrameAndVideoDuration(MultipartFile mediaFile) throws Exception {
        try (InputStream inputStream = mediaFile.getInputStream()) {
            return getVideoFirstFrameAndVideoDuration(inputStream);
        }
    }



    /**
     * 视频处理结果封装
     */

    @Getter
    private static class VideoProcessResult {

        private final byte[] coverBytes; //封面图片字节数组
        private final Integer duration;
        // 构造器
        public VideoProcessResult(byte[] coverBytes, Integer duration) {
            this.coverBytes = coverBytes;
            this.duration = duration;
        }
    }

}
