package com.shujichen.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shujichen.rag.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户信息Mapper接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}