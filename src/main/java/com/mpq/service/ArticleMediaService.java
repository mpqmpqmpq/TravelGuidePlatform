package com.mpq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.pojo.ArticleMedia;
import com.mpq.pojo.dto.ArticleMediaAddDto;
import com.mpq.pojo.dto.MediaChunkCompleteDto;
import io.minio.errors.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface ArticleMediaService extends IService<ArticleMedia> {
    ArticleMedia addArticleMedia(ArticleMediaAddDto articleMediaAddDto, Integer userId) throws Exception;


    /**
     * 根据文章ID查询所有媒体（支持按类型筛选）
     */
    List<ArticleMedia> getMediaListByArticleId(Integer articleId, String mediaType);

    /**
     * 根据媒体ID查询媒体信息
     */
    ArticleMedia getMediaById(Integer mediaId);


    /**
     * 分片上传合并并创建媒体记录
     */
    ArticleMedia completeChunkUpload(MediaChunkCompleteDto completeDto, Integer userId);

    void deleteByArticleId(Integer articleId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;
}
