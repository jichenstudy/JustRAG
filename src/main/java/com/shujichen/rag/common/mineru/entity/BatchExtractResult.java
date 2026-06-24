package com.shujichen.rag.common.mineru.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量解析结果项
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchExtractResult {

    /**
     * 文件名
     */
    @JsonProperty("file_name")
    private String fileName;

    /**
     * 任务处理状态
     * done: 完成
     * waiting-file: 等待文件上传排队提交解析任务中
     * pending: 排队中
     * running: 正在解析
     * failed: 解析失败
     * converting: 格式转换中
     */
    private String state;

    /**
     * 文件解析结果压缩包 URL，当 state=done 时有效
     */
    @JsonProperty("full_zip_url")
    private String fullZipUrl;

    /**
     * 解析失败原因，当 state=failed 时有效
     */
    @JsonProperty("err_msg")
    private String errMsg;

    /**
     * 解析对象对应的数据 ID
     */
    @JsonProperty("data_id")
    private String dataId;

    /**
     * 解析进度信息，当 state=running 时有效
     */
    @JsonProperty("extract_progress")
    private ExtractProgress extractProgress;

    /**
     * 判断任务是否完成
     */
    public boolean isDone() {
        return "done".equals(state);
    }

    /**
     * 判断任务是否失败
     */
    public boolean isFailed() {
        return "failed".equals(state);
    }

    /**
     * 判断任务是否正在运行
     */
    public boolean isRunning() {
        return "running".equals(state);
    }
}
