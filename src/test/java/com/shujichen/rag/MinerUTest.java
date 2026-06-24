package com.shujichen.rag;

import com.shujichen.rag.common.mineru.core.MinerUHttpService;
import com.shujichen.rag.common.mineru.entity.*;
import com.shujichen.rag.common.mineru.utils.MinerUResultParser;
import com.shujichen.rag.common.mineru.utils.MinerUUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.List;

/**
 * MinerU 功能测试类
 */
@Slf4j
@SpringBootTest
public class MinerUTest {

    @Autowired
    private MinerUHttpService minerUHttpService;

    /**
     * 测试提取
     */
    @Test
    public void extractTest() {
        // 调用MinerU转化为markdown
        String url = "xxx";
        ExtractTaskRequest request = ExtractTaskRequest.builder()
                .url(url)
                .isOcr(true)
                .enableFormula(true)
                .enableTable(true)
                .language("auto")
                .dataId("test-single-file-001")
                .build();
        MinerUApiResponse<TaskCreateResponse> response = minerUHttpService.createExtractTask(request);

        if (response.isSuccess()) {
            long startTime = System.currentTimeMillis();
            String taskId = response.getData().getTaskId();
            log.info("✅ 任务创建成功，任务ID: {}", taskId);

            // 同步等待任务完成
            TaskStatusResponse data = MinerUUtils.waitForTaskCompletion(taskId, 5);
            log.info("✅ 任务已完成，结果链接: {}", data.getFullZipUrl());

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.info("📊 API 调用耗时: {} ms", duration);
        }
    }

    /**
     * 测试解析
     */
    @Test
    public void processTest() throws Exception {
        String fileName = "paper.zip";
        // 获取项目根目录
        String projectRoot = System.getProperty("user.dir");
        // 构建相对于项目根目录的路径
        String zipPath = projectRoot + "/src/test/java/com/shujichen/rag/" + fileName;
        if (!new File(zipPath).exists()) {
            log.info("资源不存在，请将 {} 放到 src/test/java/com/shujichen/rag/ 目录", fileName);
            return;
        }

        // 处理 MinerU 解析结果
        MinerUParseResult result = MinerUResultParser.processResult(zipPath);

        // 显示基本信息
        String title = result.getTitle();
        String zipUrlResult = result.getZipUrl();
        log.info("📄 文档标题: {}", title);
        log.info("🔗 原始链接: {}", zipUrlResult);
        log.info("📝 内容长度: {} 字符", result.getFullContent().length());

        // 显示元数据信息
        MinerUMetadata metadata = result.getMetadata();
        if (metadata != null) {
            log.info("🖼️ 图片数量: {}", metadata.getImageCount());
        }

        // 显示主要章节
        log.info("🔍 主要章节预览:");
        List<MinerUTocItem> mainSections = result.getMainToc();
        for (MinerUTocItem section : mainSections) {
            log.info("  📋 {}: {}",
                    section.getHierarchyPath(),
                    section.hasChildren() ? section.getChildrenCount() + "个子章节" : "无子章节");
        }

        // 分析目录结构
        List<MinerUTocItem> tableOfContents = result.getTableOfContents();
        log.info("📑 文档结构分析:");
        analyzeTableOfContents(tableOfContents, 0);
    }

    /**
     * 递归分析目录结构
     */
    private static void analyzeTableOfContents(List<MinerUTocItem> tocList, int depth) {
        String indent = "  ".repeat(depth + 1);

        for (MinerUTocItem item : tocList) {
            log.info("{}📋 [H{}] {} (内容长度: {} 字符)",
                    indent, item.getLevel(), item.getTitle(), item.getContentLength());

            // 递归处理子目录
            if (item.hasChildren()) {
                analyzeTableOfContents(item.getChildren(), depth + 1);
            }
        }
    }

}
