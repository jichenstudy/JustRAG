package com.shujichen.rag.service.impl;

import com.shujichen.rag.entity.DocumentChunk;
import com.shujichen.rag.entity.KnowledgeBase;
import com.shujichen.rag.factory.VectorStoreStrategyFactory;
import com.shujichen.rag.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreServiceImpl implements VectorStoreService {

    private final VectorStoreStrategyFactory strategyFactory;

    @Override
    public void storeChunkVectors(KnowledgeBase knowledgeBase, Long documentId, List<DocumentChunk> chunks) {
        strategyFactory.getStrategy().storeChunkVectors(knowledgeBase, documentId, chunks);
    }

    @Override
    public void deleteDocumentVectors(KnowledgeBase knowledgeBase, Long documentId) {
        strategyFactory.getStrategy().deleteDocumentVectors(knowledgeBase, documentId);
    }

    @Override
    public List<Document> searchSimilar(KnowledgeBase knowledgeBase, String query, int topK) {
        return strategyFactory.getStrategy().searchSimilar(knowledgeBase, query, topK);
    }
}
