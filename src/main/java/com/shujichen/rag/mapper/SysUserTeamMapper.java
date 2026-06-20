package com.shujichen.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shujichen.rag.common.vo.user.SysUserTeamVo;
import com.shujichen.rag.entity.SysUserTeam;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户团队关联Mapper接口
 */
public interface SysUserTeamMapper extends BaseMapper<SysUserTeam> {

    /**
     * 查询用户加入的团队列表
     *
     * @param userId 用户ID
     * @return 团队列表
     */
    List<SysUserTeamVo> selectJoinTeam(@Param("userId") Long userId);

    /**
     * 查询用户加入的团队ID列表
     *
     * @param userId 用户ID
     * @return 团队ID列表
     */
    List<Long> selectJoinTeamIdList(@Param("userId") Long userId);

    /**
     * 查询团队成员列表
     *
     * @param userId 用户ID
     * @return 成员列表
     */
    List<SysUserTeamVo> selectTeamUser(@Param("userId") Long userId);
}