package com.shujichen.rag.controller.file;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shujichen.rag.common.dto.Result;
import com.shujichen.rag.entity.OssConfig;
import com.shujichen.rag.service.OssConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 对象存储配置 Controller
 */
@Tag(name = "对象存储配置管理")
@RestController
@RequestMapping("/api/system/oss/config")
@RequiredArgsConstructor
public class OssConfigController {

    private final OssConfigService ossConfigService;

    /**
     * 检查是否为超级管理员（userId == 1），否则返回无权限错误
     */
    @SuppressWarnings("unchecked")
    private <T> Result<T> checkSuperAdmin() {
        if (StpUtil.getLoginIdAsLong() != 1L) {
            return (Result<T>) Result.error(403, "仅超级管理员可操作存储配置");
        }
        return null;
    }

    /**
     * 分页查询配置
     */
    @Operation(summary = "分页查询配置")
    @GetMapping("/page")
    public Result<IPage<OssConfig>> page(
            @RequestParam(name = "page", defaultValue = "1") Integer page,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "configKey", required = false) String configKey) {
        Result<IPage<OssConfig>> check = checkSuperAdmin();
        if (check != null) return check;
        IPage<OssConfig> result = ossConfigService.selectPage(page, pageSize, configKey);
        return Result.success(result);
    }

    /**
     * 查询所有配置
     */
    @Operation(summary = "查询所有配置")
    @GetMapping("/list")
    public Result<List<OssConfig>> list() {
        Result<List<OssConfig>> check = checkSuperAdmin();
        if (check != null) return check;
        List<OssConfig> list = ossConfigService.selectList();
        return Result.success(list);
    }

    /**
     * 根据主键查询配置
     */
    @Operation(summary = "根据主键查询配置")
    @GetMapping("/{ossConfigId}")
    public Result<OssConfig> getById(
            @Parameter(description = "配置ID") @PathVariable Long ossConfigId) {
        Result<OssConfig> check = checkSuperAdmin();
        if (check != null) return check;
        OssConfig config = ossConfigService.selectById(ossConfigId);
        return Result.success(config);
    }

    /**
     * 根据 configKey 查询配置
     */
    @Operation(summary = "根据 configKey 查询配置")
    @GetMapping("/key/{configKey}")
    public Result<OssConfig> getByConfigKey(
            @Parameter(description = "配置key") @PathVariable String configKey) {
        Result<OssConfig> check = checkSuperAdmin();
        if (check != null) return check;
        OssConfig config = ossConfigService.selectByConfigKey(configKey);
        return Result.success(config);
    }

    /**
     * 查询默认配置
     */
    @Operation(summary = "查询默认配置")
    @GetMapping("/default")
    public Result<OssConfig> getDefault() {
        Result<OssConfig> check = checkSuperAdmin();
        if (check != null) return check;
        OssConfig config = ossConfigService.selectDefault();
        return Result.success(config);
    }

    /**
     * 新增配置
     */
    @Operation(summary = "新增配置")
    @PostMapping
    public Result<Void> add(@RequestBody OssConfig ossConfig) {
        Result<Void> adminCheck = checkSuperAdmin();
        if (adminCheck != null) return adminCheck;
        ossConfig.setUserId(StpUtil.getLoginIdAsLong());
        ossConfigService.insert(ossConfig);
        return Result.success();
    }

    /**
     * 修改配置
     */
    @Operation(summary = "修改配置")
    @PutMapping
    public Result<Void> edit(@RequestBody OssConfig ossConfig) {
        Result<Void> adminCheck = checkSuperAdmin();
        if (adminCheck != null) return adminCheck;
        ossConfigService.update(ossConfig);
        return Result.success();
    }

    /**
     * 删除配置
     */
    @Operation(summary = "删除配置")
    @DeleteMapping("/{ossConfigId}")
    public Result<Void> remove(
            @Parameter(description = "配置ID") @PathVariable Long ossConfigId) {
        Result<Void> adminCheck = checkSuperAdmin();
        if (adminCheck != null) return adminCheck;
        ossConfigService.deleteById(ossConfigId);
        return Result.success();
    }

    /**
     * 设置默认配置
     */
    @Operation(summary = "设置默认配置")
    @PutMapping("/default/{ossConfigId}")
    public Result<Void> setDefault(
            @Parameter(description = "配置ID") @PathVariable Long ossConfigId) {
        Result<Void> adminCheck = checkSuperAdmin();
        if (adminCheck != null) return adminCheck;
        ossConfigService.setDefault(ossConfigId);
        return Result.success();
    }
}
