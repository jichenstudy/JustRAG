package com.shujichen.rag.common.mineru.utils;

import com.shujichen.rag.common.mineru.core.MinerUHttpService;
import com.shujichen.rag.common.mineru.entity.MinerUApiResponse;
import com.shujichen.rag.common.mineru.entity.TaskStatusResponse;
import com.shujichen.rag.common.mineru.exception.MinerUException;
import com.shujichen.rag.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * MinerU 便捷工具类
 */
@Slf4j
public class MinerUUtils {

    private static final int DEFAULT_POLL_INTERVAL_SECONDS = 5; // 默认轮询间隔
    private static final int DEFAULT_MAX_WAIT_MINUTES = 30; // 默认最大等待时间

    /**
     * 等待任务完成
     *
     * @param taskId         任务 ID
     * @param maxWaitMinutes 最大等待时间（分钟）
     * @return 任务结果
     */
    public static TaskStatusResponse waitForTaskCompletion(String taskId, Integer maxWaitMinutes) {
        MinerUHttpService service = SpringUtils.getBean(MinerUHttpService.class);
        if (maxWaitMinutes == null || maxWaitMinutes < 1) {
            maxWaitMinutes = DEFAULT_MAX_WAIT_MINUTES;
        }

        long startTime = System.currentTimeMillis();
        long maxWaitMillis = maxWaitMinutes * 60 * 1000L;

        while (System.currentTimeMillis() - startTime < maxWaitMillis) {
            MinerUApiResponse<TaskStatusResponse> statusResponse = service.getTaskStatus(taskId);
            if (!statusResponse.isSuccess()) {
                log.error("查询任务状态失败，任务ID: {}, 错误码: {}, 错误信息: {}", taskId, statusResponse.getCode(), statusResponse.getMsg());
                throw new MinerUException(statusResponse.getCode(), "查询任务状态失败: " + statusResponse.getMsg());
            }

            TaskStatusResponse status = statusResponse.getData();
            log.info("任务 {} 当前状态: {}", taskId, status.getState());

            if (status.isDone()) {
                log.info("任务 {} 解析完成，结果链接: {}", taskId, status.getFullZipUrl());
                return status;
            }

            if (status.isFailed()) {
                log.error("任务解析失败，任务ID: {}, 失败原因: {}", taskId, status.getErrMsg());
                throw new MinerUException("任务解析失败: " + status.getErrMsg());
            }

            // 等待一段时间后再次查询
            try {
                Thread.sleep(DEFAULT_POLL_INTERVAL_SECONDS * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("等待任务完成被中断，任务ID: {}", taskId);
                throw new MinerUException("等待任务完成被中断", e);
            }
        }

        log.error("任务等待超时，任务ID: {}, 超时时间: {} 分钟", taskId, maxWaitMinutes);
        throw new MinerUException("任务等待超时，超过 " + maxWaitMinutes + " 分钟");
    }
}
