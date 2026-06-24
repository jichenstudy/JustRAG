package com.shujichen.rag.common.mineru.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MinerU API 通用响应封装
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MinerUApiResponse<T> {

    /**
     * 接口状态码，成功：0
     */
    private Integer code;

    /**
     * 接口处理信息，成功："ok"
     */
    private String msg;

    /**
     * 请求 ID
     */
    @JsonProperty("trace_id")
    private String traceId;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 判断响应是否成功
     */
    public boolean isSuccess() {
        return code != null && code == 0;
    }
}
