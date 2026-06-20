package com.shujichen.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.google.protobuf.ServiceException;
import com.shujichen.rag.common.dto.auth.UpdatePwdDTO;
import com.shujichen.rag.common.dto.user.SysUserAddAndUpdateDto;
import com.shujichen.rag.common.dto.user.UpdateProfileDTO;
import com.shujichen.rag.common.vo.user.SysUserProfileVo;
import com.shujichen.rag.entity.SysUser;

import java.util.List;

/**
 * 用户服务接口
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 新增用户
     *
     * @param user 用户信息
     */
    void add(SysUserAddAndUpdateDto user);

    /**
     * 更新用户
     *
     * @param user 用户信息
     */
    void update(SysUserAddAndUpdateDto user);

    /**
     * 删除用户
     *
     * @param ids 用户ID列表
     */
    void delete(List<Long> ids);

    /**
     * 修改密码
     *
     * @param updatePwdDTO 修改密码参数
     */
    void updatePwd(UpdatePwdDTO updatePwdDTO) throws ServiceException;

    /**
     * 获取个人信息
     *
     * @return 用户信息
     */
    SysUserProfileVo profile();

    /**
     * 修改个人信息（仅允许修改昵称、头像、性别、签名）
     */
    void updateProfile(UpdateProfileDTO dto);

    /**
     * 重置密码
     *
     * @param user 用户信息
     * @return 操作结果
     */
    Boolean resetPassword(SysUser user);

    /**
     * 根据用户名查询用户
     *
     * @param userName 用户名
     * @return 用户信息
     */
    SysUser getUserByName(String userName);
}
