package com.shujichen.rag.common.oss.core;

import java.io.IOException;

/**
 * 写出订阅器
 */
@FunctionalInterface
public interface WriteOutSubscriber<T> {

    void writeTo(T out) throws IOException;

}
