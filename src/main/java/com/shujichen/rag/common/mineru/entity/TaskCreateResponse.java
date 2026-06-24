package com.shujichen.rag.common.mineru.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务创建响应数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskCreateResponse {

    /**
     * 提取任务 ID，可用于查询任务结果
     */
    @JsonProperty("task_id")
    private String taskId;
}
