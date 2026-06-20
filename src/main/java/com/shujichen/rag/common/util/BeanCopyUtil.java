package com.shujichen.rag.common.util;

import org.springframework.beans.BeanUtils;

/**
 * 对象属性拷贝工具类
 */
public class BeanCopyUtil {

    /**
     * 对象属性拷贝
     *
     * @param source      源对象
     * @param targetClass 目标类
     * @param <S>         源类型
     * @param <T>         目标类型
     * @return 目标对象实例
     */
    public static <S, T> T copyObj(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        T target = null;
        try {
            target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return target;
    }
}
