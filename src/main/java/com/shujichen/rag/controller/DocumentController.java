package com.shujichen.rag.controller;

import com.shujichen.rag.common.dto.Result;
import com.shujichen.rag.common.dto.document.CreateDocumentDTO;
import com.shujichen.rag.common.dto.document.DocumentChunkDTO;
import com.shujichen.rag.common.dto.document.DocumentDTO;
import com.shujichen.rag.common.dto.document.SearchResultDTO;
import com.shujichen.rag.common.dto.file.FileDTO;
import com.shujichen.rag.common.util.BeanCopyUtil;
import com.shujichen.rag.entity.Document;
import com.shujichen.rag.entity.DocumentChunk;
import com.shujichen.rag.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档管理Controller
 */
@RestController
@RequestMapping("/api/document")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 创建文档
     *
     * @param dto 创建文档DTO
     * @return 文档ID
     */
    @PostMapping
    public Result<Long> createDocument(@Validated @RequestBody CreateDocumentDTO dto) {
        Long documentId = documentService.createDocument(
                dto.getKnowledgeBaseId(),
                dto.getFileId(),
                dto.getName(),
                dto.getDocType()
        );
        return Result.success(documentId);
    }

    /**
     * 上传文档
     *
     * @param dto 文件DTO
     * @return 上传结果列表
     */
    @PostMapping("/upload")
    public Result<List<Map<String, Object>>> uploadDocuments(@ModelAttribute FileDTO dto) {
        List<Map<String, Object>> results = documentService.uploadDocuments(
                dto.files,
                dto.knowledgeBaseId
        );
        return Result.success(results);
    }

    /**
     * 更新文档名称
     *
     * @param id   文档ID
     * @param name 新名称
     * @return 操作结果
     */
    @PutMapping("/{id}/name")
    public Result<Void> updateDocumentName(
            @PathVariable Long id,
            @RequestParam String name) {
        documentService.updateDocumentName(id, name);
        return Result.success(null);
    }

    /**
     * 删除文档
     *
     * @param id 文档ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return Result.success(null);
    }

    /**
     * 根据ID获取文档
     *
     * @param id 文档ID
     * @return 文档DTO
     */
    @GetMapping("/{id}")
    public Result<DocumentDTO> getDocumentById(@PathVariable Long id) {
        Document document = documentService.getDocumentById(id);
        DocumentDTO dto = BeanCopyUtil.copyObj(document, DocumentDTO.class);
        return Result.success(dto);
    }

    /**
     * 根据知识库ID获取文档列表
     *
     * @param knowledgeBaseId 知识库ID
     * @return 文档列表
     */
    @GetMapping("/knowledge-base/{knowledgeBaseId}")
    public Result<List<DocumentDTO>> getDocumentsByKnowledgeBaseId(@PathVariable("knowledgeBaseId") Long knowledgeBaseId) {
        List<Document> documents = documentService.getDocumentsByKnowledgeBaseId(knowledgeBaseId);
        List<DocumentDTO> dtoList = documents.stream()
                .map(doc -> BeanCopyUtil.copyObj(doc, DocumentDTO.class))
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    /**
     * 获取文档分块列表
     *
     * @param id 文档ID
     * @return 文档分块列表
     */
    @GetMapping("/{id}/chunks")
    public Result<List<DocumentChunkDTO>> getDocumentChunks(@PathVariable Long id) {
        List<DocumentChunk> chunks = documentService.getDocumentChunks(id);
        List<DocumentChunkDTO> dtoList = chunks.stream()
                .map(chunk -> BeanCopyUtil.copyObj(chunk, DocumentChunkDTO.class))
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    /**
     * 统计知识库文档数量
     *
     * @param knowledgeBaseId 知识库ID
     * @return 文档数量
     */
    @GetMapping("/knowledge-base/{knowledgeBaseId}/count")
    public Result<Long> countDocuments(@PathVariable Long knowledgeBaseId) {
        long count = documentService.countDocumentsByKnowledgeBaseId(knowledgeBaseId);
        return Result.success(count);
    }

    /**
     * 连接知识库
     *
     * @param fileId          文件ID
     * @param knowledgeBaseId 知识库ID
     * @return 是否成功
     */
    @PostMapping("/connect/knowledge-base")
    public Result<Boolean> connectKnowledgeBase(@RequestParam("fileId") String fileId,
                                                @RequestParam("knowledgeBaseId") Long knowledgeBaseId) {
        Boolean isConnected = documentService.connectKnowledgeBase(fileId, knowledgeBaseId);
        return Result.success(isConnected);
    }

    /**
     * 解析文档
     *
     * @param documentDTO 文档DTO
     * @return 操作结果
     */
    @PostMapping("/parse/document")
    public Result<Void> parseDocument(@RequestBody DocumentDTO documentDTO) {
        Boolean flag = documentService.parseDocument(documentDTO.getFileId());
        return Result.success(null);
    }

    /**
     * 相似度搜索
     *
     * @param knowledgeBaseId 知识库ID
     * @param query           查询文本
     * @param topK            返回数量
     * @return 搜索结果列表
     */
    @GetMapping("/searchSimilar")
    public Result<List<SearchResultDTO>> searchSimilar(@RequestParam("knowledgeBaseId") Long knowledgeBaseId,
                                                        @RequestParam("query") String query,
                                                        @RequestParam("topK") int topK) {
        List<org.springframework.ai.document.Document> results = documentService.searchSimilar(knowledgeBaseId, query, topK);
        List<SearchResultDTO> dtoList = results.stream()
                .map(doc -> SearchResultDTO.builder()
                        .content(doc.getText())
                        .score(doc.getScore())
                        .metadata(doc.getMetadata())
                        .build())
                .toList();

        return Result.success(dtoList);
    }
}
