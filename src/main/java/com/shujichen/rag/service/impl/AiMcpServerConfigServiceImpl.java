package com.shujichen.rag.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shujichen.rag.common.dto.mcp.McpServerDTO;
import com.shujichen.rag.common.dto.mcp.McpServerMapperDTO;
import com.shujichen.rag.common.enums.ApiMcpServerConfigType;
import com.shujichen.rag.common.vo.mcp.AiMcpServerConfigVo;
import com.shujichen.rag.entity.AiMcpServerConfig;
import com.shujichen.rag.factory.DynamicMcpManager;
import com.shujichen.rag.mapper.AiMcpServerConfigMapper;
import com.shujichen.rag.service.AiMcpServerConfigService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiMcpServerConfigServiceImpl extends ServiceImpl<AiMcpServerConfigMapper, AiMcpServerConfig>
        implements AiMcpServerConfigService {

    private final DynamicMcpManager dynamicMcpManager;

    @PostConstruct
    public void init() {
        List<AiMcpServerConfig> aiMcpServerConfigs = list(new LambdaQueryWrapper<AiMcpServerConfig>().eq(AiMcpServerConfig::getIsEnabled, 1));
        for (AiMcpServerConfig aiMcpServerConfig : aiMcpServerConfigs) {
            dynamicMcpManager.register(convertMapperDTO(aiMcpServerConfig));
        }
    }

    @Override
    public Page<AiMcpServerConfigVo> searchPage(long page, long pageSize) {
        long userId = StpUtil.getLoginIdAsLong();

        Page<AiMcpServerConfig> aiMcpServerConfigPage = new Page<>(page, pageSize);
        this.page(aiMcpServerConfigPage,new LambdaQueryWrapper<AiMcpServerConfig>()
                .eq(AiMcpServerConfig::getUserId, userId));

        Page<AiMcpServerConfigVo> aiMcpServerConfigVoPage = new Page<>();
        aiMcpServerConfigVoPage.setRecords(aiMcpServerConfigPage.getRecords().stream()
                .map(aiMcpServerConfig -> {
                    AiMcpServerConfigVo aiMcpServerConfigVo = new AiMcpServerConfigVo();
                    aiMcpServerConfigVo.setId(aiMcpServerConfig.getId());
                    aiMcpServerConfigVo.setName(aiMcpServerConfig.getName());
                    aiMcpServerConfigVo.setType(aiMcpServerConfig.getType());
                    if(ApiMcpServerConfigType.STDIO.getCode().equalsIgnoreCase(aiMcpServerConfig.getType())){
                        aiMcpServerConfigVo.setFullCommand(aiMcpServerConfig.getCommand() + " " + aiMcpServerConfig.getArgs());
                        aiMcpServerConfigVo.setEnv(aiMcpServerConfig.getEnv());
                    }
                    aiMcpServerConfigVo.setUrl(aiMcpServerConfig.getUrl());
                    aiMcpServerConfigVo.setCreatedAt(aiMcpServerConfig.getCreatedAt());
                    aiMcpServerConfigVo.setUpdatedAt(aiMcpServerConfig.getUpdatedAt());
                    aiMcpServerConfigVo.setIsEnabled(aiMcpServerConfig.getIsEnabled());

                    return aiMcpServerConfigVo;
                }).toList());
        aiMcpServerConfigVoPage.setTotal(aiMcpServerConfigPage.getTotal());
        aiMcpServerConfigVoPage.setCurrent(aiMcpServerConfigPage.getCurrent());
        aiMcpServerConfigVoPage.setSize(aiMcpServerConfigPage.getSize());
        return aiMcpServerConfigVoPage;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createMcpServer(McpServerDTO dto) {
        McpServerMapperDTO mcpServerMapperDTO = convertMapperDTO(dto);
        AiMcpServerConfig aiMcpServerConfig = convertEntityDTO(dto);
        aiMcpServerConfig.setUserId(StpUtil.getLoginIdAsLong());

        dynamicMcpManager.register(mcpServerMapperDTO);
        save(aiMcpServerConfig);
    }

    /**
     * 将前端新增/修改的DTO转为数据库实体
     * @param dto 前端新增/修改的DTO
     * @return 数据库实体
     */
    private AiMcpServerConfig convertEntityDTO(McpServerDTO dto) {
        AiMcpServerConfig aiMcpServerConfig = new AiMcpServerConfig();

        if (ApiMcpServerConfigType.STDIO.getCode().equalsIgnoreCase(dto.getType())) {
            String[] commandArray = dto.getFullCommand().split(" ");
            String command = commandArray[0];

            aiMcpServerConfig.setCommand(command);
            aiMcpServerConfig.setArgs(String.join(" ", Arrays.stream(commandArray)
                    .skip(1)
                    .toArray(String[]::new)));
        }

        aiMcpServerConfig.setId(dto.getId());
        aiMcpServerConfig.setName(dto.getName());
        aiMcpServerConfig.setType(dto.getType());
        aiMcpServerConfig.setUrl(dto.getUrl());
        aiMcpServerConfig.setEnv(dto.getEnv());
        aiMcpServerConfig.setIsEnabled(1);
        aiMcpServerConfig.setCreatedAt(LocalDateTime.now());
        aiMcpServerConfig.setUpdatedAt(LocalDateTime.now());

        return aiMcpServerConfig;
    }


    /**
     * 将前端新增/修改的DTO转为Mapper实体
     * @param dto 前端新增/修改的DTO
     * @return Mapper实体
     */
    private McpServerMapperDTO convertMapperDTO(McpServerDTO dto) {
        McpServerMapperDTO mcpServerMapperDTO = new McpServerMapperDTO();

        if (ApiMcpServerConfigType.STDIO.getCode().equalsIgnoreCase(dto.getType())) {
            String[] commandArray = dto.getFullCommand().split(" ");
            String command = commandArray[0];

            if (StrUtil.isNotBlank(dto.getEnv()) && dto.getEnv().contains("=")) {
                HashMap<String, String> envMap = new HashMap<>();
                envMap.put(dto.getEnv().split("=")[0], dto.getEnv().split("=")[1]);
                mcpServerMapperDTO.setEnv(envMap);
            }

            mcpServerMapperDTO.setCommand(command);
            mcpServerMapperDTO.setArgs(Arrays.asList(Arrays.stream(commandArray)
                    .skip(1)
                    .toArray(String[]::new)));
        }

        mcpServerMapperDTO.setName(dto.getName());
        mcpServerMapperDTO.setType(dto.getType());
        mcpServerMapperDTO.setUrl(dto.getUrl());

        return mcpServerMapperDTO;
    }

    /**
     * 将数据库实体转为Mapper实体
     * @param aiMcpServerConfig 数据库实体
     * @return Mapper实体
     */
    private McpServerMapperDTO convertMapperDTO(AiMcpServerConfig aiMcpServerConfig) {
        McpServerMapperDTO mcpServerMapperDTO = new McpServerMapperDTO();

        if (ApiMcpServerConfigType.STDIO.getCode().equalsIgnoreCase(aiMcpServerConfig.getType())) {

            if (StrUtil.isNotBlank(aiMcpServerConfig.getEnv()) && aiMcpServerConfig.getEnv().contains("=")) {
                HashMap<String, String> envMap = new HashMap<>();
                envMap.put(aiMcpServerConfig.getEnv().split("=")[0], aiMcpServerConfig.getEnv().split("=")[1]);
                mcpServerMapperDTO.setEnv(envMap);
            }

            mcpServerMapperDTO.setCommand(aiMcpServerConfig.getCommand());
            mcpServerMapperDTO.setArgs(Arrays.asList(aiMcpServerConfig.getArgs().split(" ")));
        }

        mcpServerMapperDTO.setName(aiMcpServerConfig.getName());
        mcpServerMapperDTO.setType(aiMcpServerConfig.getType());
        mcpServerMapperDTO.setUrl(aiMcpServerConfig.getUrl());

        return mcpServerMapperDTO;
    }

    @Override
    public void updateMcpServer(Long id, McpServerDTO dto) {
        McpServerMapperDTO mcpServerMapperDTO = convertMapperDTO(dto);
        AiMcpServerConfig aiMcpServerConfig = convertEntityDTO(dto);
        dynamicMcpManager.register(mcpServerMapperDTO);
        updateById(aiMcpServerConfig);
    }

    @Override
    public Boolean enabled(Long id) {
        AiMcpServerConfig serverConfig = getById(id);
        serverConfig.setIsEnabled(serverConfig.getIsEnabled() == 1 ? 0 : 1);
        if(serverConfig.getIsEnabled() == 0){
            dynamicMcpManager.unregister(serverConfig.getName());
        }else {
            dynamicMcpManager.register(convertMapperDTO(serverConfig));
        }
        return updateById(serverConfig);
    }

    @Override
    public HashMap<String, String> getAllTools(McpServerDTO dto) {
        return dynamicMcpManager.getToolsForConfig(convertMapperDTO(dto));
    }

    @Override
    public List<String> getRegisterAllToolsCount() {
        return dynamicMcpManager.listRegistered();
    }

}




