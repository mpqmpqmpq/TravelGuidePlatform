package com.mpq.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mpq.pojo.Article;

public interface ArticleRecommendService {
    Page<Article> getPersonalRecommend(int pageNum, int pageSize);
}
