package com.shujichen.rag.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shujichen.rag.common.vo.file.FileDetailVO;
import com.shujichen.rag.common.vo.file.FileInfoVO;
import com.shujichen.rag.entity.FileDetail;

import java.util.List;

/**
 * 文件详情服务接口
 */
public interface FileDetailService extends IService<FileDetail> {

    /**
     * 查询文件记录表分页列表
     *
     * @param page         页码
     * @param pageSize     每页数量
     * @param filename     文件名
     * @param hashInfo     哈希信息
     * @param uploadStatus 上传状态
     * @return 分页数据
     */
    IPage<FileDetailVO> selectPage(Integer page, Integer pageSize, String filename, String hashInfo, Integer uploadStatus);

    /**
     * 根据ID查询文件详情
     *
     * @param id 文件ID
     * @return 文件详情VO
     */
    FileDetailVO getFileDetailVO(Long id);

    /**
     * 新增文件记录表
     *
     * @param fileDetail 文件详情
     * @return 是否成功
     */
    boolean insert(FileDetail fileDetail);

    /**
     * 修改文件记录表
     *
     * @param fileDetail 文件详情
     * @return 是否成功
     */
    boolean update(FileDetail fileDetail);

    /**
     * 批量删除文件记录表
     *
     * @param ids 文件ID列表
     * @return 是否成功
     */
    boolean deleteByIds(List<String> ids);

    /**
     * 合并分片请求
     *
     * @param uploadId 上传ID
     * @return 文件信息VO
     */
    FileInfoVO completeMultipartUpload(String uploadId);

    /**
     * 校验文件
     *
     * @param md5 文件MD5值
     * @return 是否存在
     */
    Boolean checkFile(String md5);

    /**
     * 删除文件
     *
     * @param ids 文件ID列表
     * @return 是否成功
     */
    Boolean removeFile(List<String> ids);
}
