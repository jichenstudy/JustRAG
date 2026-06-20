package com.shujichen.rag.config;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.shujichen.rag.entity.SysUser;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus自动填充处理器
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时自动填充
     *
     * @param metaObject 元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        SysUser loginUserInfo = getCurrentUser();
        if (loginUserInfo != null) {
            this.strictInsertFill(metaObject, "createBy", String.class, loginUserInfo.getUsername());
            this.strictInsertFill(metaObject, "updateBy", String.class, loginUserInfo.getUsername());
        }
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新时自动填充
     *
     * @param metaObject 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        SysUser loginUserInfo = getCurrentUser();
        if (loginUserInfo != null) {
            this.strictInsertFill(metaObject, "updateBy", String.class, loginUserInfo.getUsername());
        }
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 获取当前登录用户
     *
     * @return 当前用户信息
     */
    private SysUser getCurrentUser() {
        try {
            Object obj = StpUtil.getSession().get("cur");
            if (obj instanceof SysUser) {
                return (SysUser) obj;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}