package com.shujichen.rag.common.mineru.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MinerU 解析结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MinerUParseResult {

    /**
     * 原始文件 URL
     */
    @JsonProperty("original_url")
    private String originalUrl;

    /**
     * 结果 ZIP 链接
     */
    @JsonProperty("zip_url")
    private String zipUrl;

    /**
     * 文档标题
     */
    private String title;

    /**
     * 目录结构
     */
    @JsonProperty("table_of_contents")
    private List<MinerUTocItem> tableOfContents;

    /**
     * 完整内容
     */
    @JsonProperty("full_content")
    private String fullContent;

    /**
     * 元数据信息
     */
    private MinerUMetadata metadata;

    /**
     * 获取指定级别的目录项
     */
    public List<MinerUTocItem> getTocByLevel(int level) {
        return tableOfContents.stream()
            .filter(item -> item.getLevel() == level)
            .toList();
    }

    /**
     * 获取一级目录
     */
    public List<MinerUTocItem> getMainToc() {
        return getTocByLevel(1);
    }

    /**
     * 根据标题查找目录项
     */
    public MinerUTocItem findTocByTitle(String title) {
        return findTocByTitleRecursive(tableOfContents, title);
    }

    private MinerUTocItem findTocByTitleRecursive(List<MinerUTocItem> items, String title) {
        for (MinerUTocItem item : items) {
            if (item.getTitle().contains(title)) {
                return item;
            }
            if (item.getChildren() != null && !item.getChildren().isEmpty()) {
                MinerUTocItem found = findTocByTitleRecursive(item.getChildren(), title);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

}
