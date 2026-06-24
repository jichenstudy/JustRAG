package com.shujichen.rag.common.mineru.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * MinerU 元数据信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MinerUMetadata {

    /**
     * 图片数量
     */
    @JsonProperty("image_count")
    private Integer imageCount = 0;

    /**
     * 布局信息
     */
    private JsonNode layout;

    /**
     * 内容列表
     */
    @JsonProperty("content_list")
    private JsonNode contentList;

    /**
     * 处理时间戳
     */
    @JsonProperty("processed_at")
    private Long processedAt;

    /**
     * 处理日期
     */
    @JsonProperty("processed_date")
    private String processedDate;

    /**
     * 文件大小（字节）
     */
    @JsonProperty("file_size")
    private Long fileSize;

    /**
     * 原始文件名
     */
    @JsonProperty("original_filename")
    private String originalFilename;

    /**
     * 解析耗时（毫秒）
     */
    @JsonProperty("parse_duration")
    private Long parseDuration;

    /**
     * 判断是否有布局信息
     */
    public boolean hasLayout() {
        return layout != null && !layout.isNull();
    }

    /**
     * 判断是否有内容列表
     */
    public boolean hasContentList() {
        return contentList != null && !contentList.isNull();
    }

    /**
     * 获取格式化的处理时间
     */
    public String getFormattedProcessedTime() {
        if (processedAt == null) {
            return "未知";
        }

        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(processedAt),
            ZoneId.systemDefault()
        );

        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 获取人类可读的文件大小
     */
    public String getHumanReadableFileSize() {
        if (fileSize == null || fileSize <= 0) {
            return "未知";
        }

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double size = fileSize.doubleValue();
        int unitIndex = 0;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", size, units[unitIndex]);
    }

    /**
     * 获取人类可读的解析耗时
     */
    public String getHumanReadableParseDuration() {
        if (parseDuration == null || parseDuration <= 0) {
            return "未知";
        }

        long seconds = parseDuration / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%d小时%d分钟%d秒", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%d分钟%d秒", minutes, seconds % 60);
        } else {
            return String.format("%d秒", seconds);
        }
    }

    /**
     * 获取统计摘要
     */
    public String getStatsSummary() {
        StringBuilder summary = new StringBuilder();

        if (imageCount != null && imageCount > 0) {
            summary.append(String.format("图片:%d张", imageCount));
        }

        if (hasLayout()) {
            if (summary.length() > 0) {
                summary.append(", ");
            }
            summary.append("包含布局信息");
        }

        if (hasContentList()) {
            if (summary.length() > 0) {
                summary.append(", ");
            }
            summary.append("包含内容列表");
        }

        if (fileSize != null && fileSize > 0) {
            if (summary.length() > 0) {
                summary.append(", ");
            }
            summary.append("大小:").append(getHumanReadableFileSize());
        }

        return summary.length() > 0 ? summary.toString() : "无统计信息";
    }

    /**
     * 设置处理开始时间（用于计算耗时）
     */
    public void markProcessingStart() {
        this.processedAt = System.currentTimeMillis();
        this.processedDate = getFormattedProcessedTime();
    }

    /**
     * 设置处理完成时间并计算耗时
     */
    public void markProcessingEnd(long startTime) {
        long endTime = System.currentTimeMillis();
        this.parseDuration = endTime - startTime;
        if (this.processedAt == null) {
            this.processedAt = endTime;
            this.processedDate = getFormattedProcessedTime();
        }
    }
}
