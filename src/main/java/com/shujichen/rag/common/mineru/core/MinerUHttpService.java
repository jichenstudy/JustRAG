package com.shujichen.rag.common.mineru.core;

import com.shujichen.rag.common.mineru.entity.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * MinerU 提取服务
 */
public interface MinerUHttpService {

    // ========== 本地化 API（保留兼容性）==========

    /**
     * 本地化文件解析（保留兼容性）
     */
    @PostExchange(url = "/file_parse", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    FileParseResult fileParse(@RequestPart("files") List<MultipartFile> files,
                              @RequestPart("return_md") Boolean returnMd,
                              @RequestPart("return_middle_json") Boolean returnMiddleJson,
                              @RequestPart("return_model_output") Boolean returnModelOutput,
                              @RequestPart("return_content_list") Boolean returnContentList,
                              @RequestPart("return_images") Boolean returnImages);

    // ========== 云端 API v4 ==========

    /**
     * 创建单个文件解析任务
     *
     * @param request 解析任务请求参数
     * @return 任务创建响应
     */
    @PostExchange(url = "/api/v4/extract/task", contentType = MediaType.APPLICATION_JSON_VALUE)
    MinerUApiResponse<TaskCreateResponse> createExtractTask(@RequestBody ExtractTaskRequest request);

    /**
     * 获取任务状态和结果
     *
     * @param taskId 任务 ID
     * @return 任务状态响应
     */
    @GetExchange(url = "/api/v4/extract/task/{taskId}")
    MinerUApiResponse<TaskStatusResponse> getTaskStatus(@PathVariable String taskId);

    /**
     * 申请批量文件上传链接
     *
     * @param request 批量文件上传请求参数
     * @return 批量上传响应
     */
    @PostExchange(url = "/api/v4/file-urls/batch", contentType = MediaType.APPLICATION_JSON_VALUE)
    MinerUApiResponse<BatchUploadResponse> createBatchFileUpload(@RequestBody BatchFileUploadRequest request);

    /**
     * 创建批量 URL 解析任务
     *
     * @param request 批量 URL 解析请求参数
     * @return 批量任务响应
     */
    @PostExchange(url = "/api/v4/extract/task/batch", contentType = MediaType.APPLICATION_JSON_VALUE)
    MinerUApiResponse<BatchTaskResponse> createBatchUrlTask(@RequestBody BatchUrlRequest request);

    /**
     * 获取批量任务结果
     *
     * @param batchId 批量任务 ID
     * @return 批量结果响应
     */
    @GetExchange(url = "/api/v4/extract-results/batch/{batchId}")
    MinerUApiResponse<BatchResultResponse> getBatchResults(@PathVariable String batchId);
}
