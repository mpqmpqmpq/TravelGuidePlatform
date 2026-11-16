package com.mpq.service;

import com.mpq.pojo.User;
import io.minio.errors.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface AvatarService {

    /*
    * 上传头像
    * */
    User updateAvatar(Integer userId, byte[] fileBytes, String originalFilename, String contentType) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;
    /*
    * 验证头像内容
    * */
    void validateAvatarContent(byte[] fileBytes,String originalFilename, String contentType);
    /*
    * 删除头像
    * */
    void deleteAvatar(User user);
}
