package com.shujichen.rag.controller;

import com.shujichen.rag.common.dto.Result;
import com.shujichen.rag.common.dto.knowledgebase.CreateKnowledgeBaseDTO;
import com.shujichen.rag.common.dto.knowledgebase.KnowledgeBaseDTO;
import com.shujichen.rag.common.dto.knowledgebase.UpdateKnowledgeBaseDTO;
import com.shujichen.rag.common.util.BeanCopyUtil;
import com.shujichen.rag.entity.KnowledgeBase;
import com.shujichen.rag.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库控制器
 */
@RestController
@RequestMapping("/api/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 创建知识库
     *
     * @param dto 创建参数
     * @return 知识库ID
     */
    @PostMapping
    public Result<Long> createKnowledgeBase(@Validated @RequestBody CreateKnowledgeBaseDTO dto) {
        Long id = knowledgeBaseService.createKnowledgeBase(
                dto.getName(),
                dto.getDescription(),
                dto.getModelId(),
                dto.getChunkStrategy(),
                dto.getChunkSize(),
                dto.getChunkOverlap(),
                dto.getChunkMinSize()
        );
        return Result.success(id);
    }

    /**
     * 更新知识库
     *
     * @param id  知识库ID
     * @param dto 更新参数
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public Result<Void> updateKnowledgeBase(
            @PathVariable Long id,
            @RequestBody UpdateKnowledgeBaseDTO dto) {
        knowledgeBaseService.updateKnowledgeBase(
                id,
                dto.getName(),
                dto.getDescription(),
                dto.getModelId(),
                dto.getChunkStrategy(),
                dto.getChunkSize(),
                dto.getChunkOverlap(),
                dto.getChunkMinSize()
        );
        return Result.success(null);
    }

    /**
     * 删除知识库
     *
     * @param id 知识库ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteKnowledgeBase(@PathVariable Long id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return Result.success(null);
    }

    /**
     * 根据ID获取知识库详情
     *
     * @param id 知识库ID
     * @return 知识库信息
     */
    @GetMapping("/{id}")
    public Result<KnowledgeBaseDTO> getKnowledgeBaseById(@PathVariable("id") Long id) {
        KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBaseById(id);
        KnowledgeBaseDTO dto = BeanCopyUtil.copyObj(knowledgeBase, KnowledgeBaseDTO.class);
        return Result.success(dto);
    }

    /**
     * 获取所有知识库列表
     *
     * @return 知识库列表
     */
    @GetMapping
    public Result<List<KnowledgeBaseDTO>> getAllKnowledgeBases() {
        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.getAllKnowledgeBases();
        List<KnowledgeBaseDTO> dtoList = knowledgeBases.stream()
                .map(kb -> BeanCopyUtil.copyObj(kb, KnowledgeBaseDTO.class))
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }
}
