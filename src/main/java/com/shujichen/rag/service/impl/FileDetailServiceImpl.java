package com.shujichen.rag.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shujichen.rag.common.oss.core.OssClient;
import com.shujichen.rag.common.oss.enums.AccessPolicyType;
import com.shujichen.rag.common.oss.factory.OssFactory;
import com.shujichen.rag.common.vo.file.FileDetailVO;
import com.shujichen.rag.common.vo.file.FileInfoVO;
import com.shujichen.rag.entity.FileDetail;
import com.shujichen.rag.mapper.FileDetailMapper;
import com.shujichen.rag.service.FileDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 文件记录表 服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileDetailServiceImpl extends ServiceImpl<FileDetailMapper, FileDetail> implements FileDetailService {

    /**
     * 查询文件记录表分页列表
     */
    @Override
    public IPage<FileDetailVO> selectPage(Integer page, Integer pageSize, String filename, String hashInfo, Integer uploadStatus) {
        Long userId = StpUtil.getLoginIdAsLong();
        Page<FileDetailVO> pageParam = new Page<>(page, pageSize);

        IPage<FileDetailVO> result = baseMapper.selectPageWithKnowledgeBaseName(
            pageParam, filename, hashInfo, uploadStatus, userId
        );

        // 处理私有URL
        result.getRecords().forEach(this::matchingUrl);

        return result;
    }

    /**
     * 根据ID查询文件详情
     */
    @Override
    public FileDetailVO getFileDetailVO(Long id) {
        FileDetailVO vo = baseMapper.selectByIdWithKnowledgeBaseName(id);
        if (vo != null) {
            matchingUrl(vo);
        }
        return vo;
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
        // TODO: 分片上传功能已移除，此方法需要重新实现或移除
        throw new UnsupportedOperationException("分片上传功能已移除");
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
                // 使用 OssFactory 删除文件
                objectNameList.forEach(objectName -> {
                    try {
                        OssFactory.instance().delete(objectName);
                    } catch (Exception e) {
                        log.error("删除 OSS 文件失败: {}", objectName, e);
                    }
                });
            });
            removeByIds(ids);
        } catch (Exception e) {
            log.error("删除文件失败：{}", e);
            return false;
        }
        return true;
    }

    /**
     * 桶类型为 private 的URL 修改为临时URL时长为120s
     *
     * @param vo 文件详情VO
     */
    private void matchingUrl(FileDetailVO vo) {
        try {
            OssClient storage = OssFactory.instance(vo.getPlatform());
            // 仅修改桶类型为 private 的URL，临时URL时长为120s
            if (AccessPolicyType.PRIVATE == storage.getAccessPolicy()) {
                vo.setUrl(storage.createPresignedGetUrl(vo.getFilename(), Duration.ofSeconds(120)));
            }
        } catch (Exception e) {
            log.warn("处理文件URL失败，文件ID: {}, 错误: {}", vo.getId(), e.getMessage());
        }
    }
}
