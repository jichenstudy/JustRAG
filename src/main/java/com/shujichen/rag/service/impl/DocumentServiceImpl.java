package com.shujichen.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shujichen.rag.common.enums.ParseStatus;
import com.shujichen.rag.entity.Document;
import com.shujichen.rag.entity.DocumentChunk;
import com.shujichen.rag.entity.FileDetail;
import com.shujichen.rag.entity.KnowledgeBase;
import com.shujichen.rag.mapper.DocumentChunkMapper;
import com.shujichen.rag.mapper.DocumentMapper;
import com.shujichen.rag.mapper.KnowledgeBaseMapper;
import com.shujichen.rag.service.DocumentService;
import com.shujichen.rag.service.FileDetailService;
import com.shujichen.rag.service.VectorStoreService;
import com.shujichen.rag.splitting.DocumentParser;
import com.shujichen.rag.splitting.MarkdownSplittingService;
import com.shujichen.rag.util.MinioUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl extends ServiceImpl<DocumentMapper, Document>
        implements DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final VectorStoreService vectorStoreService;
    private final FileDetailService fileDetailService;
    private final MinioUtil minioUtil;
    private final MarkdownSplittingService splittingService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadDocument(MultipartFile file, Long knowledgeBaseId, Boolean shouldParse) {
        Long documentId = null;
        try {
            String docType = getDocTypeFromFilename(file.getOriginalFilename());
            documentId = createDocument(knowledgeBaseId, "", file.getOriginalFilename(), docType);

            KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);

            if (Boolean.TRUE.equals(shouldParse)) {
                // 标记为解析中
                markDocumentAsParsing(documentId);

                String content = DocumentParser.parse(file);
                List<DocumentChunk> chunks = splittingService.split(
                        documentId,
                        content,
                        getChunkStrategy(knowledgeBase),
                        getChunkSize(knowledgeBase),
                        getChunkOverlap(knowledgeBase),
                        getChunkMinSize(knowledgeBase));
                saveDocumentChunks(documentId, chunks);
                vectorStoreService.storeChunkVectors(knowledgeBase, documentId, chunks);
            }

            return documentId;

        } catch (Exception e) {
            log.error("文档上传处理失败,knowledgeBaseId: {}, filename: {}", knowledgeBaseId, file.getOriginalFilename(), e);

            if (documentId != null && Boolean.TRUE.equals(shouldParse)) {
                try {
                    markDocumentAsParseFailed(documentId);
                } catch (Exception ex) {
                    log.error("标记文档解析失败时出错,documentId: {}", documentId, ex);
                }
            }

            throw new RuntimeException("文档上传处理失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDocument(Long knowledgeBaseId, String fileId, String name, String docType) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在,ID: " + knowledgeBaseId);
        }

        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getFileId, fileId);
        Document existingDoc = documentMapper.selectOne(queryWrapper);
        if (existingDoc != null) {
            throw new IllegalArgumentException("该文件已关联到文档,文档ID: " + existingDoc.getId());
        }

        Document document = new Document();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setFileId(fileId);
        document.setName(name);
        document.setDocType(docType);
        document.setParseStatus(ParseStatus.UPLOADED.getCode());
        document.setChunkCount(0);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        documentMapper.insert(document);

        log.info("文档创建成功,文档ID: {}, 知识库ID: {}, 文件ID: {}", document.getId(), knowledgeBaseId, fileId);
        return document.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDocumentChunks(Long documentId, List<DocumentChunk> chunks) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在,ID: " + documentId);
        }

        LambdaQueryWrapper<DocumentChunk> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(DocumentChunk::getDocumentId, documentId);
        documentChunkMapper.delete(deleteWrapper);

        for (DocumentChunk chunk : chunks) {
            documentChunkMapper.insert(chunk);
        }

        document.setParseStatus(ParseStatus.PARSED.getCode());
        document.setChunkCount(chunks.size());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);

        log.info("文档分块保存成功,文档ID: {}, 分块数量: {}", documentId, chunks.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markDocumentAsParseFailed(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在,ID: " + documentId);
        }

        document.setParseStatus(ParseStatus.FAILED.getCode());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);

        log.warn("文档解析失败,文档ID: {}", documentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDocumentName(Long documentId, String newName) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在,ID: " + documentId);
        }

        document.setName(newName);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);

        log.info("文档名称更新成功,文档ID: {}, 新名称: {}", documentId, newName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在,ID: " + documentId);
        }

        // 获取知识库信息用于删除向量
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(document.getKnowledgeBaseId());
        if (knowledgeBase != null) {
            try {
                vectorStoreService.deleteDocumentVectors(knowledgeBase, documentId);
            } catch (Exception e) {
                log.error("删除向量数据失败,documentId: {}", documentId, e);
            }
        }

        LambdaQueryWrapper<DocumentChunk> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(DocumentChunk::getDocumentId, documentId);
        documentChunkMapper.delete(deleteWrapper);

        documentMapper.deleteById(documentId);

        log.info("文档删除成功,文档ID: {}", documentId);
    }

    @Override
    public Document getDocumentById(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在,ID: " + documentId);
        }
        return document;
    }

    @Override
    public List<Document> getDocumentsByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId);
        return documentMapper.selectList(queryWrapper);
    }

    @Override
    public List<DocumentChunk> getDocumentChunks(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在,ID: " + documentId);
        }

        LambdaQueryWrapper<DocumentChunk> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DocumentChunk::getDocumentId, documentId);
        queryWrapper.orderByAsc(DocumentChunk::getChunkIndex);
        return documentChunkMapper.selectList(queryWrapper);
    }

    @Override
    public long countDocumentsByKnowledgeBaseId(Long knowledgeBaseId) {
        LambdaQueryWrapper<Document> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Document::getKnowledgeBaseId, knowledgeBaseId);
        return documentMapper.selectCount(queryWrapper);
    }

    @Override
    public Boolean connectKnowledgeBase(String fileId, Long knowledgeBaseId) {
        FileDetail fileDetail = fileDetailService.getById(fileId);
        if (fileDetail == null) {
            return false;
        }

        Document document = getDocumentByFileId(fileId);
        if (document != null) {
            return false;
        }

        document = new Document();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setFileId(fileId);
        document.setName(fileDetail.getFilename());
        document.setDocType(getDocTypeFromFilename(fileDetail.getFilename()));
        document.setParseStatus(ParseStatus.UPLOADED.getCode());
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.insert(document);

        fileDetail.setKnowledgeBaseId(knowledgeBaseId);
        fileDetailService.updateById(fileDetail);
        return true;
    }

    private Document getDocumentByFileId(String fileId) {
        return getOne(new LambdaQueryWrapper<Document>()
                .eq(Document::getFileId, fileId));
    }

    @Transactional
    @Override
    public Boolean parseDocument(String fileId) {
        Document document = getDocumentByFileId(fileId);
        Long documentId = document.getId();
        try {
            // 标记为解析中
            markDocumentAsParsing(documentId);

            FileDetail fileDetail = fileDetailService.getById(document.getFileId());
            KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(document.getKnowledgeBaseId());

            String content = DocumentParser.parse(
                    minioUtil.downloadFile(fileDetail.getBucketName(), fileDetail.getObjectName()),
                    fileDetail.getFilename());

            // 分片切割
            List<DocumentChunk> chunks = splittingService.split(
                    documentId,
                    content,
                    getChunkStrategy(knowledgeBase),
                    getChunkSize(knowledgeBase),
                    getChunkOverlap(knowledgeBase),
                    getChunkMinSize(knowledgeBase));
            saveDocumentChunks(documentId, chunks);
            vectorStoreService.storeChunkVectors(knowledgeBase, documentId, chunks);

            document.setChunkCount(chunks.size());
            document.setParseStatus(ParseStatus.PARSED.getCode());
            documentMapper.updateById(document);
        } catch (Exception ex) {
            markDocumentAsParseFailed(documentId);
            log.error("标记文档解析失败时出错,documentId: {},fileName: {}", documentId, document.getName(), ex);
            throw new RuntimeException("文档上传处理失败: " + ex.getMessage(), ex);
        }
        return true;
    }

    @Override
    public List<org.springframework.ai.document.Document> searchSimilar(Long knowledgeBaseId, String query, int topK) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        return vectorStoreService.searchSimilar(knowledgeBase, query, topK);
    }

    private String getDocTypeFromFilename(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "TXT";
        }
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> "PDF";
            case "doc", "docx" -> "WORD";
            case "xls", "xlsx" -> "EXCEL";
            case "md", "markdown" -> "MARKDOWN";
            case "html", "htm" -> "HTML";
            default -> "TXT";
        };
    }

    /**
     * 标记文档为解析中状态（向量化过程展示）
     */
    private void markDocumentAsParsing(Long documentId) {
        Document document = documentMapper.selectById(documentId);
        if (document != null) {
            document.setParseStatus(ParseStatus.PARSING.getCode());
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(document);
        }
    }

    private String getChunkStrategy(KnowledgeBase kb) {
        return kb.getChunkStrategy() != null ? kb.getChunkStrategy() : "smart";
    }

    private int getChunkSize(KnowledgeBase kb) {
        return kb.getChunkSize() != null ? kb.getChunkSize() : 1000;
    }

    private int getChunkOverlap(KnowledgeBase kb) {
        return kb.getChunkOverlap() != null ? kb.getChunkOverlap() : 200;
    }

    private int getChunkMinSize(KnowledgeBase kb) {
        return kb.getChunkMinSize() != null ? kb.getChunkMinSize() : 100;
    }
}
