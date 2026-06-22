package com.shujichen.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shujichen.rag.common.vo.file.FileDetailVO;
import com.shujichen.rag.entity.FileDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文件详情Mapper接口
 */
@Mapper
public interface FileDetailMapper extends BaseMapper<FileDetail> {

    /**
     * 分页查询文件详情（关联知识库名称）
     *
     * @param page         分页参数
     * @param filename     文件名（模糊查询）
     * @param hashInfo     文件哈希
     * @param uploadStatus 上传状态
     * @param userId       用户ID
     * @return 分页结果
     */
    IPage<FileDetailVO> selectPageWithKnowledgeBaseName(
            Page<FileDetailVO> page,
            @Param("filename") String filename,
            @Param("hashInfo") String hashInfo,
            @Param("uploadStatus") Integer uploadStatus,
            @Param("userId") Long userId
    );

    /**
     * 根据ID查询文件详情（关联知识库名称）
     *
     * @param id 文件ID
     * @return 文件详情VO
     */
    FileDetailVO selectByIdWithKnowledgeBaseName(@Param("id") Long id);
}