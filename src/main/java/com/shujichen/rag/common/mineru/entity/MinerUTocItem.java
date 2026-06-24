package com.shujichen.rag.common.mineru.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * MinerU 目录项
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MinerUTocItem {

    /**
     * 目录级别（1-6）
     */
    private int level;

    /**
     * 目录标题
     */
    private String title;

    /**
     * 目录内容
     */
    private String content;

    /**
     * 子目录
     */
    private List<MinerUTocItem> children = new ArrayList<>();

    public MinerUTocItem(int level, String title, String content) {
        this.level = level;
        this.title = title;
        this.content = content;
        this.children = new ArrayList<>();
    }

    /**
     * 判断是否有子目录
     */
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    /**
     * 获取子目录数量
     */
    public int getChildrenCount() {
        return children != null ? children.size() : 0;
    }

    /**
     * 添加子目录
     */
    public void addChild(MinerUTocItem child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }

    /**
     * 获取指定级别的所有子目录（递归）
     */
    public List<MinerUTocItem> getChildrenByLevel(int targetLevel) {
        List<MinerUTocItem> result = new ArrayList<>();
        collectChildrenByLevel(this, targetLevel, result);
        return result;
    }

    private void collectChildrenByLevel(MinerUTocItem item, int targetLevel, List<MinerUTocItem> result) {
        if (item.getLevel() == targetLevel) {
            result.add(item);
        }
        if (item.getChildren() != null) {
            for (MinerUTocItem child : item.getChildren()) {
                collectChildrenByLevel(child, targetLevel, result);
            }
        }
    }

    /**
     * 获取内容摘要（前100个字符）
     */
    public String getContentSummary() {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }
        String trimmed = content.trim();
        return trimmed.length() > 100 ? trimmed.substring(0, 100) + "..." : trimmed;
    }

    /**
     * 获取内容长度
     */
    public int getContentLength() {
        return content != null ? content.trim().length() : 0;
    }

    /**
     * 判断是否为主要章节（一级或二级标题）
     */
    public boolean isMainSection() {
        return level <= 2;
    }

    /**
     * 获取层级路径（用于显示完整路径）
     */
    public String getHierarchyPath() {
        return "H" + level + ": " + title;
    }

    /**
     * 根据标题搜索子目录
     */
    public MinerUTocItem findChildByTitle(String searchTitle) {
        if (children == null) {
            return null;
        }

        for (MinerUTocItem child : children) {
            if (child.getTitle().contains(searchTitle)) {
                return child;
            }
            // 递归搜索
            MinerUTocItem found = child.findChildByTitle(searchTitle);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 获取所有子目录的标题列表（用于快速预览）
     */
    public List<String> getAllChildTitles() {
        List<String> titles = new ArrayList<>();
        if (children != null) {
            for (MinerUTocItem child : children) {
                titles.add(child.getTitle());
                titles.addAll(child.getAllChildTitles());
            }
        }
        return titles;
    }
}
