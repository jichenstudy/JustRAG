package com.shujichen.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shujichen.rag.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档分块Mapper接口
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {
}