package com.shujichen.rag.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.protobuf.ServiceException;
import com.shujichen.rag.common.dto.auth.UpdatePwdDTO;
import com.shujichen.rag.common.dto.user.SysUserAddAndUpdateDto;
import com.shujichen.rag.common.dto.user.UpdateProfileDTO;
import com.shujichen.rag.common.vo.user.SysUserProfileVo;
import com.shujichen.rag.entity.SysUser;
import com.shujichen.rag.entity.SysUserTeam;
import com.shujichen.rag.mapper.SysUserMapper;
import com.shujichen.rag.mapper.SysUserTeamMapper;
import com.shujichen.rag.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务实现类
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysUserTeamMapper userTeamMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(SysUserAddAndUpdateDto sysUserAddAndUpdateDto) {
        SysUser user = sysUserAddAndUpdateDto.getUser();
        if (selectByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        save(user);

        SysUserTeam sysUserTeam = new SysUserTeam();
        sysUserTeam.setUserId(user.getId());
        sysUserTeam.setTeamId(user.getId());
        sysUserTeam.setCreateAt(LocalDateTime.now());
        userTeamMapper.insert(sysUserTeam);
    }

    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    private boolean selectByUsername(String username) {
        return count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SysUserAddAndUpdateDto user) {
        if (getById(user.getUser().getId()) == null) {
            throw new RuntimeException("用户不存在");
        }
        updateById(user.getUser());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        removeBatchByIds(ids);
    }

    @Override
    public void updatePwd(UpdatePwdDTO updatePwdDTO) throws ServiceException {
        SysUser user = this.getById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        if (user.getId() != StpUtil.getLoginIdAsLong()) {
            throw new ServiceException("只能修改自己的密码！");
        }

        if (!BCrypt.checkpw(updatePwdDTO.getOldPassword(), user.getPassword())) {
            throw new ServiceException("旧密码错误");
        }

        user.setPassword(BCrypt.hashpw(updatePwdDTO.getNewPassword(), BCrypt.gensalt()));
        this.updateById(user);
    }

    @Override
    public SysUserProfileVo profile() {
        SysUser sysUser = baseMapper.selectById(StpUtil.getLoginIdAsLong());
        sysUser.setPassword(null);
        return SysUserProfileVo.builder().sysUser(sysUser).build();
    }

    @Override
    public void updateProfile(UpdateProfileDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = baseMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getSex() != null) {
            user.setSex(dto.getSex());
        }
        if (dto.getSignature() != null) {
            user.setSignature(dto.getSignature());
        }
        // 邮箱和手机号仅当用户未设置时允许填写一次
        if (dto.getEmail() != null && (user.getEmail() == null || user.getEmail().isBlank())) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getMobile() != null && (user.getMobile() == null || user.getMobile().isBlank())) {
            user.setMobile(dto.getMobile());
        }
        baseMapper.updateById(user);
    }

    @Override
    public Boolean resetPassword(SysUser user) {
        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        baseMapper.updateById(user);
        return true;
    }

    @Override
    public SysUser getUserByName(String userName) {
        return getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, userName));
    }
}
