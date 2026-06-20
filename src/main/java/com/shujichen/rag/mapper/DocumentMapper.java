package com.shujichen.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shujichen.rag.entity.Document;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档Mapper接口
 */
@Mapper
public interface DocumentMapper extends BaseMapper<Document> {
}