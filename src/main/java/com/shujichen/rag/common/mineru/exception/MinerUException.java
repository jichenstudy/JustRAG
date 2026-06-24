package com.shujichen.rag.common.mineru.exception;

/**
 * MinerU 服务异常
 */
public class MinerUException extends RuntimeException {

    private final Integer errorCode;

    public MinerUException(String message) {
        super(message);
        this.errorCode = null;
    }

    public MinerUException(Integer errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public MinerUException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public MinerUException(Integer errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    @Override
    public String toString() {
        if (errorCode != null) {
            return "MinerUException{" +
                "errorCode=" + errorCode +
                ", message='" + getMessage() + '\'' +
                '}';
        }
        return super.toString();
    }
}
