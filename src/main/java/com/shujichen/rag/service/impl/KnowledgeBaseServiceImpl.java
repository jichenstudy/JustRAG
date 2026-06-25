package com.shujichen.rag.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shujichen.rag.entity.KnowledgeBase;
import com.shujichen.rag.factory.VectorStoreStrategyFactory;
import com.shujichen.rag.mapper.KnowledgeBaseMapper;
import com.shujichen.rag.mapper.SysUserTeamMapper;
import com.shujichen.rag.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final VectorStoreStrategyFactory vectorStoreStrategyFactory;
    private final SysUserTeamMapper sysUserTeamMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createKnowledgeBase(String name, String description, Long embeddingModelId, Long visionModelId,
                                    String chunkStrategy, Integer chunkSize,
                                    Integer chunkOverlap, Integer chunkMinSize) {
        // 检查名称是否已存在
        LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeBase::getName, name);
        if (knowledgeBaseMapper.selectCount(queryWrapper) > 0) {
            throw new IllegalArgumentException("知识库名称已存在: " + name);
        }

        // 创建新集合
        String collectionName = "kb_" + IdUtil.fastSimpleUUID();
        VectorStore orCreateVectorStore = vectorStoreStrategyFactory.getStrategy().getOrCreateVectorStore(embeddingModelId, collectionName);
        if (orCreateVectorStore == null) {
            throw new IllegalArgumentException("创建集合失败");
        }
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName(name);
        knowledgeBase.setDescription(description);
        knowledgeBase.setEmbeddingModelId(embeddingModelId);
        knowledgeBase.setVisionModelId(visionModelId);
        knowledgeBase.setCollectionsName(collectionName);
        knowledgeBase.setChunkStrategy(chunkStrategy != null ? chunkStrategy : "smart");
        knowledgeBase.setChunkSize(chunkSize != null ? chunkSize : 1000);
        knowledgeBase.setChunkOverlap(chunkOverlap != null ? chunkOverlap : 200);
        knowledgeBase.setChunkMinSize(chunkMinSize != null ? chunkMinSize : 100);
        knowledgeBase.setTeamId(StpUtil.getLoginIdAsLong());
        knowledgeBase.setCreatedAt(LocalDateTime.now());
        knowledgeBase.setUpdatedAt(LocalDateTime.now());

        knowledgeBaseMapper.insert(knowledgeBase);

        log.info("知识库创建成功,ID: {}, 名称: {}", knowledgeBase.getId(), name);
        return knowledgeBase.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateKnowledgeBase(Long id, String name, String description, Long embeddingModelId, Long visionModelId,
                                    String chunkStrategy, Integer chunkSize,
                                    Integer chunkOverlap, Integer chunkMinSize) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(id);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在,ID: " + id);
        }

        if (name != null && !name.equals(knowledgeBase.getName())) {
            LambdaQueryWrapper<KnowledgeBase> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(KnowledgeBase::getName, name);
            if (knowledgeBaseMapper.selectCount(queryWrapper) > 0) {
                throw new IllegalArgumentException("知识库名称已存在: " + name);
            }
            knowledgeBase.setName(name);
        }

        if (description != null) {
            knowledgeBase.setDescription(description);
        }

        knowledgeBase.setEmbeddingModelId(embeddingModelId);
        knowledgeBase.setVisionModelId(visionModelId);
        if (chunkStrategy != null) knowledgeBase.setChunkStrategy(chunkStrategy);
        if (chunkSize != null) knowledgeBase.setChunkSize(chunkSize);
        if (chunkOverlap != null) knowledgeBase.setChunkOverlap(chunkOverlap);
        if (chunkMinSize != null) knowledgeBase.setChunkMinSize(chunkMinSize);
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.updateById(knowledgeBase);

        log.info("知识库更新成功,ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableKnowledgeBase(Long id) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(id);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在,ID: " + id);
        }

        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.updateById(knowledgeBase);

        log.info("知识库已启用,ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableKnowledgeBase(Long id) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(id);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在,ID: " + id);
        }

        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseMapper.updateById(knowledgeBase);

        log.info("知识库已停用,ID: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long id) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(id);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在,ID: " + id);
        }

        // 删除向量库 collection
        String collectionName = knowledgeBase.getCollectionsName();
        if (collectionName != null && !collectionName.isBlank()) {
            boolean dropped = vectorStoreStrategyFactory.getStrategy().dropCollection(collectionName);
            if (!dropped) {
                log.warn("删除向量库 collection 失败，但继续删除知识库记录，collection: {}", collectionName);
            }
        }

        knowledgeBaseMapper.deleteById(id);
        log.info("知识库删除成功,ID: {}, collection: {}", id, collectionName);
    }

    @Override
    public KnowledgeBase getKnowledgeBaseById(Long id) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(id);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在,ID: " + id);
        }
        return knowledgeBase;
    }

    @Override
    public List<KnowledgeBase> getAllKnowledgeBases() {
        List<Long> sysUserTeamVos = sysUserTeamMapper.selectJoinTeamIdList(StpUtil.getLoginIdAsLong());
        if(sysUserTeamVos.isEmpty()){
            return Collections.emptyList();
        }
        return knowledgeBaseMapper.selectList(new LambdaQueryWrapper<KnowledgeBase>()
                .in(KnowledgeBase::getTeamId, sysUserTeamVos));
    }


}
