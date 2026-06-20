package com.shujichen.rag.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shujichen.rag.common.vo.file.FileInfoVO;
import com.shujichen.rag.entity.FileDetail;
import com.shujichen.rag.mapper.FileDetailMapper;
import com.shujichen.rag.service.FileDetailService;
import com.shujichen.rag.util.MinioUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 文件记录表 服务实现类
 */
@Service
@RequiredArgsConstructor
public class FileDetailServiceImpl extends ServiceImpl<FileDetailMapper, FileDetail> implements FileDetailService {

    private final MinioUtil minioUtil;

    /**
     * 查询文件记录表分页列表
     */
    @Override
    public IPage<FileDetail> selectPage(Integer page, Integer pageSize, String filename, String hashInfo, Integer uploadStatus) {
        LambdaQueryWrapper<FileDetail> wrapper = new LambdaQueryWrapper<>();
        // 构建查询条件
        wrapper.eq(StrUtil.isNotBlank(filename), FileDetail::getFilename, filename);
        wrapper.eq(StrUtil.isNotBlank(hashInfo), FileDetail::getHashInfo, hashInfo);
        wrapper.eq(Objects.nonNull(uploadStatus), FileDetail::getUploadStatus, uploadStatus);
        wrapper.eq(FileDetail::getUserId, StpUtil.getLoginIdAsLong());

        return page(new Page<>(page, pageSize), wrapper);
    }

    /**
     * 新增文件记录表
     */
    @Override
    public boolean insert(FileDetail fileDetail) {
        return save(fileDetail);
    }

    /**
     * 修改文件记录表
     */
    @Override
    public boolean update(FileDetail fileDetail) {
        return updateById(fileDetail);
    }

    /**
     * 批量删除文件记录表
     */
    @Override
    public boolean deleteByIds(List<String> ids) {
        return removeByIds(ids);
    }

    @Transactional
    @Override
    public FileInfoVO completeMultipartUpload(String uploadId) {
        FileInfoVO fileInfo = minioUtil.completeMultipartUpload(uploadId);
        String objectName = fileInfo.getObjectName();

        FileDetail fileDetail = new FileDetail()
                .setUrl(fileInfo.getUrl())
                .setSize(fileInfo.getFileSize())
                .setFilename(fileInfo.getFileName())
                .setOriginalFilename(fileInfo.getOriginalFileName())
                .setBucketName(fileInfo.getBucketName())
                .setObjectName(objectName)
                .setBasePath(fileInfo.getObjectName())
                .setPath(fileInfo.getBucketName() + "/" + fileInfo.getObjectName())
                .setExt(fileInfo.getFileExtension())
                .setContentType(fileInfo.getContentType())
                .setPlatform("minio")
                .setHashInfo(fileInfo.getFileHash())
                .setUploadStatus("COMPLETED".equals(fileInfo.getStatus()) ? 1 : 0)
                .setUploadId(fileInfo.getUploadId())
                .setUserId(StpUtil.getLoginIdAsLong())
                .setCreateTime(LocalDateTime.now());
        save(fileDetail);
        fileInfo.setFileId(fileDetail.getId());
        return fileInfo;
    }

    @Override
    public Boolean checkFile(String md5) {
        return count(new LambdaQueryWrapper<FileDetail>()
                .eq(FileDetail::getHashInfo, md5)) > 0;
    }

    @Transactional
    @Override
    public Boolean removeFile(List<String> ids) {
        List<FileDetail> fileDetails = listByIds(ids);
        try {
            Optional.ofNullable(fileDetails).ifPresent(res -> {
                List<String> objectNameList = fileDetails.stream().map(FileDetail::getBasePath).collect(Collectors.toList());
                minioUtil.deleteFiles(objectNameList);
            });
            removeByIds(ids);
        } catch (Exception e) {
            log.error("删除文件失败：{}", e);
            return false;
        }
        return true;
    }
}
