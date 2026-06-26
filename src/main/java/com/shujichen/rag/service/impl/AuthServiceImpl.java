package com.shujichen.rag.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shujichen.rag.common.constant.Constants;
import com.shujichen.rag.common.dto.auth.LoginDTO;
import com.shujichen.rag.common.dto.auth.LoginUserInfo;
import com.shujichen.rag.common.dto.auth.RegisterDTO;
import com.shujichen.rag.common.dto.auth.ResetPasswordDTO;
import com.shujichen.rag.common.util.*;
import com.shujichen.rag.config.properties.MailProperties;
import com.shujichen.rag.entity.SysUser;
import com.shujichen.rag.entity.SysUserTeam;
import com.shujichen.rag.mapper.SysUserMapper;
import com.shujichen.rag.mapper.SysUserTeamMapper;
import com.shujichen.rag.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysUserTeamMapper userTeamMapper;
    private final MailProperties mailProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginUserInfo login(LoginDTO loginDTO) {
        log.info("用户登录请求: account={}", loginDTO.getUsername());

        // 1. 查询用户
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.and(wrapper -> wrapper
                .eq(SysUser::getUsername, loginDTO.getUsername())
                .or()
                .eq(SysUser::getEmail, loginDTO.getUsername()));
        SysUser user = sysUserMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 验证密码
        if (!BCrypt.checkpw(loginDTO.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 检查状态
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用");
        }

        // 4. 执行登录
        StpUtil.login(user.getId());
        String tokenValue = StpUtil.getTokenValue();

        // 5. 更新登录信息
        // 获取客户端IP
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        final UserAgent userAgent = UserAgentUtil.parse(request.getHeader("User-Agent"));
        final String ip = JakartaServletUtil.getClientIP(request);
        // 获取客户端操作系统
        String os = userAgent.getOs().getName();
        // 获取客户端浏览器
        String browser = userAgent.getBrowser().getName();
        // 获取真实地址
        String address = AddressUtils.getRealAddressByIP(ip);

        user.setIp(ip);
        user.setOs(os);
        user.setBrowser(browser);
        user.setIpLocation(address);
        user.setLoginType("PASSWORD");
        user.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        // 6. 构建返回信息
        LoginUserInfo loginUserInfo = BeanCopyUtil.copyObj(user, LoginUserInfo.class);
        loginUserInfo.setToken(tokenValue);

        // 7. 存储到Session
        StpUtil.getSession().set("USER", user);

        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return loginUserInfo;
    }

    @Override
    public void sendEmailCode(String email) {
        // 检查邮箱是否已注册
        Long emailCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, email));
        if (emailCount > 0) {
            throw new RuntimeException("该邮箱已注册");
        }

        // 生成4位随机验证码
        String code = RandomUtil.randomNumbers(4);
        String key = Constants.CAPTCHA_CODE_KEY + email;

        // 存储到Redis，有效期5分钟
        RedisUtils.setCacheObject(key, code, Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));

        // 发送邮件
        try {
            String content = "您的验证码为：" + code + "\n有效期" + Constants.CAPTCHA_EXPIRATION + "分钟，请尽快填写。";
            MailUtils.sendText(email, "JustRAG 注册用户", content);
            log.info("验证码发送成功: email={}", email);
        } catch (Exception e) {
            log.error("验证码发送异常: email={}, error={}", email, e.getMessage());
            throw new RuntimeException("验证码发送失败，请稍后重试");
        }
    }

    @Override
    public void sendResetPasswordCode(String email) {
        // 检查邮箱是否已注册
        Long emailCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, email));
        if (emailCount == 0) {
            throw new RuntimeException("该邮箱未注册");
        }

        // 生成4位随机验证码
        String code = RandomUtil.randomNumbers(4);
        String key = Constants.RESET_PWD_CODE_KEY + email;

        // 存储到Redis，有效期5分钟
        RedisUtils.setCacheObject(key, code, Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));

        // 发送邮件
        try {
            String content = "您的验证码为：" + code + "\n有效期" + Constants.CAPTCHA_EXPIRATION + "分钟，请尽快填写。";
            MailUtils.sendText(email, "JustRAG 找回密码", content);
            log.info("找回密码验证码发送成功: email={}", email);
        } catch (Exception e) {
            log.error("找回密码验证码发送异常: email={}, error={}", email, e.getMessage());
            throw new RuntimeException("验证码发送失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordDTO dto) {
        // 1. 验证邮箱验证码
        String key = Constants.RESET_PWD_CODE_KEY + dto.getEmail();
        String cachedCode = RedisUtils.getCacheObject(key);
        if (cachedCode == null || !cachedCode.equals(dto.getCaptchaCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        // 2. 查找用户
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, dto.getEmail()));
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 3. 更新密码
        user.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        sysUserMapper.updateById(user);

        // 4. 删除已使用的验证码
        RedisUtils.deleteObject(key);

        log.info("用户重置密码成功: userId={}, email={}", user.getId(), user.getEmail());
    }

    @Override
    public boolean isEmailCaptchaEnabled() {
        return mailProperties.getEnabled();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean register(RegisterDTO dto) {
        // 1. 验证邮箱验证码（如果提供了验证码）
        if (dto.getCaptchaCode() != null && !dto.getCaptchaCode().isEmpty()) {
            String key = Constants.CAPTCHA_CODE_KEY + dto.getEmail();
            String cachedCode = RedisUtils.getCacheObject(key);
            if (cachedCode == null || !cachedCode.equals(dto.getCaptchaCode())) {
                throw new RuntimeException("验证码错误或已过期");
            }
            // 删除已使用的验证码
            RedisUtils.deleteObject(key);
        }

        // 2. 检查用户名是否已存在
        if (checkUsernameExists(dto.getUsername())) {
            throw new RuntimeException("用户名已存在，请更换");
        }

        // 3. 检查邮箱是否已注册
        Long emailCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, dto.getEmail()));
        if (emailCount > 0) {
            throw new RuntimeException("该邮箱已注册，请前往登录");
        }

        // 4. 根据用户名获取头像
        String avatar = AvatarUtil.getAvatarByUsername(dto.getUsername());

        // 5. 创建用户
        SysUser sysUser = SysUser.builder()
                .username(dto.getUsername())
                .password(BCrypt.hashpw(dto.getPassword()))
                .nickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername())
                .email(dto.getEmail())
                .avatar(avatar)
                .status(1)
                .build();
        sysUserMapper.insert(sysUser);

        // 6. 创建相应的团队信息
        SysUserTeam sysUserTeam = new SysUserTeam();
        sysUserTeam.setUserId(sysUser.getId());
        sysUserTeam.setTeamId(sysUser.getId());
        sysUserTeam.setCreateAt(LocalDateTime.now());
        userTeamMapper.insert(sysUserTeam);

        log.info("用户注册成功: userId={}, username={}, email={}", sysUser.getId(), sysUser.getUsername(), sysUser.getEmail());
        return true;
    }

    @Override
    public boolean checkUsernameExists(String username) {
        return sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)) > 0;
    }

    @Override
    public LoginUserInfo getLoginUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        return BeanCopyUtil.copyObj(user, LoginUserInfo.class);
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }
}
