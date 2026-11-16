package com.mpq.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mpq.common.Result;
import com.mpq.pojo.Destination;
import io.minio.errors.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

public interface DestinationService extends IService<Destination> {
    Page<Destination> getList(Page<Destination> page, String province);
    List<Destination> getHot(int size);
    Page<Destination> search(Page<Destination> page, String keyword);
    void incrementViewCount(Integer id);

    Result addDestination(Destination destination);

    Result updateByDestinationId(Destination destination) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;

    Result updateViewById(Integer id);

    Set<String> getProvince();
}