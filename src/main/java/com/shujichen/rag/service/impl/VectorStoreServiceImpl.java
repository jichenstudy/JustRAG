package com.shujichen.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shujichen.rag.entity.DocumentChunk;
import com.shujichen.rag.entity.KnowledgeBase;
import com.shujichen.rag.factory.RagVectorStoreManager;
import com.shujichen.rag.mapper.DocumentChunkMapper;
import com.shujichen.rag.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreServiceImpl implements VectorStoreService {

    private final DocumentChunkMapper documentChunkMapper;
    private final RagVectorStoreManager ragVectorStoreManager;

    @Override
    public void storeChunkVectors(KnowledgeBase knowledgeBase, Long documentId, List<DocumentChunk> chunks) {
        VectorStore vectorStore = ragVectorStoreManager.getVectorStore(knowledgeBase);
        if (vectorStore == null) {
            log.warn("VectorStore 不可用，跳过向量存储，documentId: {}, collection: {}",
                    documentId, knowledgeBase.getCollectionsName());
            return;
        }

        if (chunks == null || chunks.isEmpty()) {
            log.warn("文档分块列表为空，跳过向量存储，documentId: {}", documentId);
            return;
        }

        List<Document> documents = chunks.stream()
                .map(chunk -> {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("documentId", documentId);
                    metadata.put("chunkId", chunk.getId());
                    metadata.put("chunkIndex", chunk.getChunkIndex());
                    metadata.put("knowledgeBaseId", knowledgeBase.getId());

                    return new Document(
                            String.valueOf(chunk.getId()),
                            chunk.getContent(),
                            metadata
                    );
                })
                .toList();

        // DashScope Embedding API 限制一次最多处理 10 个文本，需要分批处理
        int batchSize = 8;
        int totalBatches = (documents.size() + batchSize - 1) / batchSize;

        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            List<Document> batch = documents.subList(i, end);
            vectorStore.add(batch);
            int currentBatch = (i / batchSize) + 1;
            log.debug("向量存储批次 {}/{} 完成，documentId: {}, 本批数量: {}",
                    currentBatch, totalBatches, documentId, batch.size());
        }

        log.info("向量存储成功，documentId: {}, collection: {}, 分块数量: {}, 批次数: {}",
                documentId, knowledgeBase.getCollectionsName(), chunks.size(), totalBatches);
    }

    @Override
    public void deleteDocumentVectors(KnowledgeBase knowledgeBase, Long documentId) {
        VectorStore vectorStore = ragVectorStoreManager.getVectorStore(knowledgeBase);
        if (vectorStore == null) {
            log.warn("VectorStore 不可用，跳过向量删除，documentId: {}, collection: {}",
                    documentId, knowledgeBase.getCollectionsName());
            return;
        }

        try {
            LambdaQueryWrapper<DocumentChunk> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(DocumentChunk::getDocumentId, documentId);
            List<DocumentChunk> chunks = documentChunkMapper.selectList(queryWrapper);

            if (chunks.isEmpty()) {
                log.info("文档没有分块，无需删除向量，documentId: {}", documentId);
                return;
            }

            List<String> chunkIds = chunks.stream()
                    .map(chunk -> String.valueOf(chunk.getId()))
                    .toList();

            vectorStore.delete(chunkIds);
            log.info("向量删除成功，documentId: {}, collection: {}, 删除数量: {}",
                    documentId, knowledgeBase.getCollectionsName(), chunkIds.size());
        } catch (Exception e) {
            log.error("删除文档向量失败，documentId: {}, collection: {}",
                    documentId, knowledgeBase.getCollectionsName(), e);
            throw new RuntimeException("删除文档向量失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Document> searchSimilar(KnowledgeBase knowledgeBase, String query, int topK) {
        VectorStore vectorStore = ragVectorStoreManager.getVectorStore(knowledgeBase);
        if (vectorStore == null) {
            log.warn("VectorStore 不可用，跳过向量搜索，collection: {}, query: {}",
                    knowledgeBase.getCollectionsName(), query);
            return Collections.emptyList();
        }

        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(topK)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.info("向量搜索成功，collection: {}, query: {}, 结果数量: {}",
                    knowledgeBase.getCollectionsName(), query, results.size());
            return results;

        } catch (Exception e) {
            log.error("向量搜索失败，collection: {}, query: {}",
                    knowledgeBase.getCollectionsName(), query, e);
            throw new RuntimeException("向量搜索失败: " + e.getMessage(), e);
        }
    }
}
