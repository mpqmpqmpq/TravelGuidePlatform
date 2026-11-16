package com.mpq.controller;

import com.mpq.common.Result;
import com.mpq.pojo.ArticleMedia;
import com.mpq.pojo.dto.ArticleMediaAddDto;
import com.mpq.pojo.dto.MediaChunkCompleteDto;
import com.mpq.pojo.dto.MediaChunkInitDto;
import com.mpq.pojo.dto.MediaChunkUploadDto;
import com.mpq.service.ArticleMediaService;
import com.mpq.service.MinioMultipartService;
import com.mpq.service.MinioService;
import com.mpq.utils.UserContextHolder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/articleMedia")
public class ArticleMediaController {
    private final ArticleMediaService articleMediaService;
    private final MinioService minioService;
    private final MinioMultipartService minioMultipartService;
    private static final String ARTICLE_MEDIA_BUCKET = "article-media";


    @PostMapping("/add")
    public Result addArticleMedia(@ModelAttribute ArticleMediaAddDto articleMediaAddDto) {
        try {
            Integer userId = UserContextHolder.getUserId();
            if (userId == null) {
                return Result.unauthorized("请先登录");
            }
            ArticleMedia articleMedia = articleMediaService.addArticleMedia(articleMediaAddDto, userId);
            return Result.success("上传成功", articleMedia);
        } catch (Exception e) {
            log.error("普通上传媒体异常:", e);
            return Result.failure(500, "系统异常:" + e.getMessage());
        }
    }

    @GetMapping("/{mediaId}")
    public void getMediaFileStream(@PathVariable Integer mediaId, HttpServletResponse response) {
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            // 1. 查询媒体并校验状态
            ArticleMedia media = articleMediaService.getMediaById(mediaId);
            if (media == null || media.getStatus() != 1) {
                response.sendError(404, "媒体文件不存在或已失效");
                return;
            }
            // 2. 解析MinIO对象名（从mediaUrl中提取）
            String objectName = minioService.parseObjectNameFromUrl(media.getMediaUrl());
            // 3. 优化ContentType：视频用明确的video/mp4，避免浏览器无法预览
            String contentType = "image".equals(media.getMediaType())
                    ? MediaType.IMAGE_JPEG_VALUE
                    : "video/mp4";
            response.setContentType(contentType);
            // 支持浏览器直接预览（而非下载）
            response.setHeader("Content-Disposition", "inline; filename=\"" + media.getMediaTitle() + "\"");

            // 4. 流传输（8KB缓冲区，减少IO次数）
            inputStream = minioService.getFileInputStream(objectName);
            // 5. 响应
            outputStream = response.getOutputStream();
            byte[] buffer = new byte[1024 * 8];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
        } catch (Exception e) {
            log.error("获取媒体流失败，mediaId: {}", mediaId, e);
            try {
                response.sendError(500, "获取文件失败");
            } catch (Exception ex) {
                log.error("响应错误处理失败", ex);
            }
        } finally {
            // 强制关闭流，避免资源泄漏
            try {
                if (outputStream != null) outputStream.close();
                if (inputStream != null) inputStream.close();
            } catch (Exception e) {
                log.error("关闭流失败", e);
            }
        }
    }

    @GetMapping("/videoCover/{mediaId}")
    public void getVideoCoverStream(@PathVariable Integer mediaId, HttpServletResponse response) {
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            ArticleMedia media = articleMediaService.getMediaById(mediaId);
            // 校验：媒体存在 + 状态正常 + 是视频类型（确保有封面）
            if (media == null || media.getStatus() != 1 || !"video".equals(media.getMediaType())) {
                response.sendError(404, "视频封面不存在或已失效");
                return;
            }
            String objectName = minioService.parseObjectNameFromUrl(media.getVideoCoverUrl());
            response.setContentType(MediaType.IMAGE_JPEG_VALUE);
            response.setHeader("Content-Disposition", "inline; filename=\"" + media.getMediaTitle() + "_cover.jpg\"");

            inputStream = minioService.getFileInputStream(objectName);
            outputStream = response.getOutputStream();
            byte[] buffer = new byte[1024 * 8];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
        } catch (Exception e) {
            log.error("获取视频封面流失败，mediaId: {}", mediaId, e);
            try {
                response.sendError(500, "获取封面失败");
            } catch (Exception ex) {
                log.error("响应错误处理失败", ex);
            }
        } finally {
            try {
                if (outputStream != null) outputStream.close();
                if (inputStream != null) inputStream.close();
            } catch (Exception e) {
                log.error("关闭流失败", e);
            }
        }
    }


    /**
     *  初始化分片上传
     * 生成uploadId、规划objectName，返回给前端用于后续分片上传
     */
    @PostMapping("/chunk/init")
    public Result initChunkUpload(@RequestBody MediaChunkInitDto initDto) {
        try {
            // 1. 登录校验（与普通上传逻辑一致）
            Integer userId = UserContextHolder.getUserId();
            if (userId == null) {
                return Result.unauthorized("请先登录");
            }
            // 2. 核心参数校验
            if (initDto.getArticleId() == null) {
                return Result.failure(400, "攻略ID不能为空");
            }
            if (initDto.getFileName() == null || initDto.getFileName().trim().isEmpty()) {
                return Result.failure(400, "原始文件名不能为空");
            }
            if (initDto.getContentType() == null || initDto.getContentType().trim().isEmpty()) {
                return Result.failure(400, "文件类型（contentType）不能为空");
            }
            if (initDto.getFileSize() == null || initDto.getFileSize() <= 0) {
                return Result.failure(400, "文件大小必须大于0");
            }

            // 3. 生成与普通上传一致的objectName
            // 规则：article/media/[类型路径]/[时间戳]_[原始文件名]
            boolean isVideo = initDto.getContentType().startsWith("video/");
            String typePath = isVideo ? "video/" : "image/";
            String objectName = String.format(
                    "article/media/%s%d_%s",
                    typePath,
                    System.currentTimeMillis(), // 时间戳
                    initDto.getFileName().replaceAll(" ", "_") // 替换空格，
            );

            // 4. 调用Minio分片服务，生成uploadId
            String uploadId = minioMultipartService.initMultipartUpload(
                    ARTICLE_MEDIA_BUCKET,
                    objectName,
                    initDto.getContentType()
            );

            // 5. 返回结果
            return Result.success("分片初始化成功", new java.util.HashMap<String, Object>() {{
                put("uploadId", uploadId); // 分片唯一标识
                put("objectName", objectName); // MinIO中的目标文件名
                put("chunkSize", 5 * 1024 * 1024); // 分片大小：5MB
                put("totalChunks", (int) Math.ceil((double) initDto.getFileSize() / (5 * 1024 * 1024))); // 总分片数
            }});
        } catch (Exception e) {
            log.error("分片初始化异常，articleId: {}", initDto.getArticleId(), e);
            return Result.failure(500, "分片初始化失败:" + e.getMessage());
        }
    }

    /**
     * 上传单个分片
     * 接收前端的单个分片，暂存到MinIO，返回ETag用于合并
     */
    @PostMapping(value = "/chunk/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result uploadChunk(@ModelAttribute MediaChunkUploadDto uploadDto) {
        try {
            // 1. 登录校验
            if (UserContextHolder.getUserId() == null) {
                return Result.unauthorized("请先登录");
            }
            // 2. 核心参数校验
            if (uploadDto.getArticleId() == null) {
                return Result.failure(400, "攻略ID不能为空");
            }
            if (uploadDto.getUploadId() == null || uploadDto.getUploadId().trim().isEmpty()) {
                return Result.failure(400, "分片标识uploadId不能为空");
            }
            if (uploadDto.getFileName() == null || uploadDto.getFileName().trim().isEmpty()) {
                return Result.failure(400, "目标文件名（fileName，即objectName）不能为空");
            }
            if (uploadDto.getChunkIndex() == null || uploadDto.getChunkIndex() < 0) {
                return Result.failure(400, "分片索引chunkIndex必须大于等于0");
            }
            if (uploadDto.getTotalChunks() == null || uploadDto.getTotalChunks() <= 0) {
                return Result.failure(400, "总分片数totalChunks必须大于0");
            }
            if (uploadDto.getChunkFile() == null || uploadDto.getChunkFile().isEmpty()) {
                return Result.failure(400, "分片文件chunkFile不能为空或空文件");
            }

            // 3. 调用Minio分片服务，上传当前分片并获取ETag
            String etag = minioMultipartService.uploadChunk(
                    ARTICLE_MEDIA_BUCKET,
                    uploadDto.getFileName(), // MinIO的objectName
                    uploadDto.getUploadId(),
                    uploadDto.getChunkIndex(),
                    uploadDto.getChunkFile()
            );

            // 4. 返回当前分片的ETag,前端需按索引顺序保存，合并时需传完整etagList
            return Result.success("分片上传成功", new java.util.HashMap<String, Object>() {{
                put("etag", etag); // 分片的ETag
                put("chunkIndex", uploadDto.getChunkIndex()); // 分片索引
            }});
        } catch (Exception e) {
            log.error("分片上传异常，uploadId: {}, chunkIndex: {}", uploadDto.getUploadId(), uploadDto.getChunkIndex(), e);
            return Result.failure(500, "分片上传失败:" + e.getMessage());
        }
    }

    /**
     * 合并分片并创建媒体记录
     * 合并所有分片为完整文件，同时在数据库创建article_media记录
     */
    @PostMapping("/chunk/complete")
    public Result completeChunkUpload(@RequestBody MediaChunkCompleteDto completeDto) {
        try {
            // 1. 登录校验
            Integer userId = UserContextHolder.getUserId();
            if (userId == null) {
                return Result.unauthorized("请先登录");
            }
            // 2. 核心参数校验（基于已有DTO的字段）
            if (completeDto.getArticleId() == null) {
                return Result.failure(400, "攻略ID不能为空");
            }
            if (completeDto.getUploadId() == null || completeDto.getUploadId().trim().isEmpty()) {
                return Result.failure(400, "分片标识uploadId不能为空");
            }
            if (completeDto.getFileName() == null || completeDto.getFileName().trim().isEmpty()) {
                return Result.failure(400, "目标文件名（fileName，即objectName）不能为空");
            }
            if (completeDto.getEtagList() == null || completeDto.getEtagList().isEmpty()) {
                return Result.failure(400, "ETag列表etagList不能为空（需包含所有分片的ETag）");
            }
            if (completeDto.getContentType() == null || completeDto.getContentType().trim().isEmpty()) {
                return Result.failure(400, "文件类型contentType不能为空");
            }

            ArticleMedia articleMedia = articleMediaService.completeChunkUpload(completeDto, userId);

            // 返回创建的媒体记录
            return Result.success("分片合并成功，媒体上传完成", articleMedia);
        } catch (Exception e) {
            log.error("分片合并异常，articleId: {}, uploadId: {}", completeDto.getArticleId(), completeDto.getUploadId(), e);
            return Result.failure(500, "分片合并失败:" + e.getMessage());
        }
    }

    /**
     *  取消分片上传
     * 上传中断/失败时，清理MinIO临时分片，避免资源浪费
     */
    @PostMapping("/chunk/abort")
    public Result abortChunkUpload(@RequestBody java.util.Map<String, String> params) {
        try {
            // 1. 登录校验
            if (UserContextHolder.getUserId() == null) {
                return Result.unauthorized("请先登录");
            }
            // 2. 提取并校验参数
            String uploadId = params.get("uploadId");
            String objectName = params.get("objectName");
            if (uploadId == null || uploadId.trim().isEmpty()) {
                return Result.failure(400, "分片标识uploadId不能为空");
            }
            if (objectName == null || objectName.trim().isEmpty()) {
                return Result.failure(400, "目标文件名objectName不能为空");
            }

            // 3. 调用Minio服务取消分片，清理临时文件
            minioMultipartService.abortMultipartUpload(
                    ARTICLE_MEDIA_BUCKET,
                    objectName,
                    uploadId
            );

            return Result.success("取消分片成功，已清理MinIO临时分片");
        } catch (Exception e) {
            log.error("取消分片异常，uploadId: {}", params.get("uploadId"), e);
            return Result.failure(500, "取消分片失败:" + e.getMessage());
        }
    }

    /**
     * 删除攻略媒体
     */
    @DeleteMapping("/delete/{mediaId}")
    public Result deleteArticleMedia(@PathVariable Integer mediaId) {
        try {
            // 1. 登录校验
            Integer userId = UserContextHolder.getUserId();
            if (userId == null) {
                return Result.unauthorized("请先登录");
            }

            // 2. 查询媒体信息（校验存在性+权限）
            ArticleMedia media = articleMediaService.getMediaById(mediaId);
            if (media == null) {
                return Result.failure(404, "媒体文件不存在");
            }
            // 权限校验：只能删除自己上传的媒体
            if (!media.getUploadUserId().equals(userId)) {
                return Result.failure(403, "无权删除该媒体");
            }

            // 3. 先删除MinIO中的文件（主文件+视频封面）
            // 3.1 删除媒体主文件
            String mediaObjectName = minioService.parseObjectNameFromUrl(media.getMediaUrl());
            minioService.deleteFile(ARTICLE_MEDIA_BUCKET, mediaObjectName);

            // 3.2 若为视频，额外删除封面文件
            if ("video".equals(media.getMediaType()) && media.getVideoCoverUrl() != null) {
                String coverObjectName = minioService.parseObjectNameFromUrl(media.getVideoCoverUrl());
                minioService.deleteFile(ARTICLE_MEDIA_BUCKET, coverObjectName);
            }

            // 4. 删除数据库记录
            articleMediaService.removeById(mediaId);

            return Result.success("媒体删除成功");
        } catch (Exception e) {
            log.error("删除媒体异常，mediaId: {}", mediaId, e);
            return Result.failure(500, "删除媒体失败: " + e.getMessage());
        }
    }

    /**
     * 获取媒体
     */
    @GetMapping("/media/{articleId}")
    public Result getMediaByArticleId(@PathVariable Integer articleId) {
        String type = "video";
        List<ArticleMedia> mediaListByArticleId = articleMediaService.getMediaListByArticleId(articleId, type);
        return Result.success("获取媒体成功", mediaListByArticleId);
    }
}