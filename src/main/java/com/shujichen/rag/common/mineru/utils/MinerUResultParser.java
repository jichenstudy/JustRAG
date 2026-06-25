package com.shujichen.rag.common.mineru.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shujichen.rag.common.mineru.entity.MinerUMetadata;
import com.shujichen.rag.common.mineru.entity.MinerUParseResult;
import com.shujichen.rag.common.mineru.entity.MinerUTocItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * MinerU 解析结果处理工具类
 */
@Slf4j
public class MinerUResultParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final WebClient webClient = WebClient.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(200 * 1024 * 1024))
        .build();


    /**
     * 处理 MinerU 解析结果
     *
     * @param zipUrl 结果 ZIP 链接或本地文件路径，为空时仅返回默认结果
     * @return 解析后的结果对象
     * @throws Exception 处理异常
     */
    public static MinerUParseResult processResult(String zipUrl) throws Exception {
        log.info("开始处理 MinerU 解析结果: {}", zipUrl);

        long startTime = System.currentTimeMillis();

        // 如果 zipUrl 为空，仅返回默认结果
        if (zipUrl == null || zipUrl.isBlank()) {
            log.info("zipUrl 为空，返回默认结果");
            MinerUParseResult result = new MinerUParseResult();
            result.setTitle("Untitled Document");
            result.setTableOfContents(new ArrayList<>());
            result.setFullContent("");
            return result;
        }

        Path tempDir = Files.createTempDirectory("mineru_result_");

        try {
            Path zipFile;
            Path extractDir;

            if (zipUrl.startsWith("http://") || zipUrl.startsWith("https://")) {
                // URL 方式：下载 ZIP 文件
                zipFile = downloadZipFile(zipUrl, tempDir);
                extractDir = extractZipFile(zipFile, tempDir);
            } else {
                // 本地文件路径方式：直接解压
                Path localPath = Path.of(zipUrl);
                if (!Files.exists(localPath)) {
                    throw new FileNotFoundException("本地文件不存在: " + zipUrl);
                }
                log.info("使用本地文件: {}", localPath);
                extractDir = extractZipFile(localPath, tempDir);
            }

            // 查找 full.md 文件
            Path fullMdFile = findFullMdFile(extractDir);
            if (fullMdFile == null) {
                throw new FileNotFoundException("未找到 full.md 文件");
            }

            // 解析 Markdown 内容
            MinerUParseResult result = parseMarkdownFile(fullMdFile);
            result.setZipUrl(zipUrl);

            // 处理图片上传（在临时文件清理前）
            try {
                String baseDir = extractDir.toString();
                MarkdownImageUploader.ImageUploadResult imageResult = MarkdownImageUploader.processMarkdown(result.getFullContent(), baseDir);

                if (imageResult.getUploadedCount() > 0) {
                    result.setFullContent(imageResult.getProcessedMarkdown());
                    log.info("✅ 已处理并上传 {} 张图片到OSS", imageResult.getUploadedCount());
                } else if (imageResult.hasErrors()) {
                    log.warn("⚠️ 图片处理有 {} 个错误", imageResult.getErrors().size());
                    imageResult.getErrors().forEach(error -> log.warn("  - {}", error));
                }
            } catch (Exception e) {
                log.warn("⚠️ 图片处理过程中出现异常: {}", e.getMessage());
            }

            // 读取其他元数据文件
            enrichWithMetadata(extractDir, result, startTime);

            log.info("MinerU 结果处理完成: {}", result.getTitle());
            return result;

        } finally {
            // 清理临时文件
            cleanupTempFiles(tempDir);
        }
    }

    /**
     * 下载 ZIP 文件
     */
    private static Path downloadZipFile(String zipUrl, Path tempDir) throws Exception {
        log.info("正在下载文件: {}", zipUrl);

        Path zipFile = tempDir.resolve("result.zip");

        byte[] zipData = webClient.get()
            .uri(zipUrl)
            .retrieve()
            .bodyToMono(byte[].class)
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
            .block(Duration.ofMinutes(10));

        if (zipData == null) {
            throw new IOException("下载文件失败");
        }

        Files.write(zipFile, zipData);
        log.info("文件下载完成: {} ({} bytes)", zipFile, zipData.length);

        return zipFile;
    }

    /**
     * 解压 ZIP 文件
     */
    private static Path extractZipFile(Path zipFile, Path tempDir) throws Exception {
        log.info("正在解压文件: {}", zipFile);

        Path extractDir = tempDir.resolve("extracted");
        Files.createDirectories(extractDir);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = extractDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }

        log.info("文件解压完成: {}", extractDir);
        return extractDir;
    }

    /**
     * 查找 full.md 文件
     */
    private static Path findFullMdFile(Path extractDir) throws IOException {
        return Files.walk(extractDir)
            .filter(path -> path.getFileName().toString().equals("full.md"))
            .findFirst()
            .orElse(null);
    }

    /**
     * 解析 Markdown 文件
     */
    private static MinerUParseResult parseMarkdownFile(Path mdFile) throws IOException {
        log.info("正在解析 Markdown 文件: {}", mdFile);

        List<String> lines = Files.readAllLines(mdFile, StandardCharsets.UTF_8);
        String fullContent = String.join("\n", lines);

        MinerUParseResult result = new MinerUParseResult();
        result.setFullContent(fullContent);

        // 解析目录结构
        List<MinerUTocItem> toc = parseTableOfContents(lines);
        result.setTableOfContents(toc);

        // 提取标题（第一个一级标题）
        String title = extractTitle(lines);
        result.setTitle(title);

        log.info("Markdown 解析完成，找到 {} 个目录项", toc.size());
        return result;
    }

    /**
     * 解析目录结构
     */
    private static List<MinerUTocItem> parseTableOfContents(List<String> lines) {
        List<MinerUTocItem> flatToc = new ArrayList<>();
        Pattern headerPattern = Pattern.compile("^(#{1,6})\\s+(.*)$");

        MinerUTocItem currentItem = null;
        StringBuilder contentBuilder = new StringBuilder();

        for (String line : lines) {
            Matcher matcher = headerPattern.matcher(line);
            if (matcher.matches()) {
                // 保存当前项的内容
                if (currentItem != null) {
                    currentItem.setContent(contentBuilder.toString().trim());
                    flatToc.add(currentItem);
                }

                // 创建新的目录项
                int level = matcher.group(1).length();
                String title = matcher.group(2).trim();
                currentItem = new MinerUTocItem(level, title, "");
                contentBuilder = new StringBuilder();
            } else {
                // 累积内容
                if (contentBuilder.length() > 0) {
                    contentBuilder.append("\n");
                }
                contentBuilder.append(line);
            }
        }

        // 保存最后一个项目
        if (currentItem != null) {
            currentItem.setContent(contentBuilder.toString().trim());
            flatToc.add(currentItem);
        }

        // 构建层级结构
        return buildHierarchicalToc(flatToc);
    }

    /**
     * 构建层级目录结构
     */
    private static List<MinerUTocItem> buildHierarchicalToc(List<MinerUTocItem> flatToc) {
        List<MinerUTocItem> result = new ArrayList<>();
        Stack<MinerUTocItem> stack = new Stack<>();

        for (MinerUTocItem item : flatToc) {
            // 弹出比当前项级别高或相等的项
            while (!stack.isEmpty() && stack.peek().getLevel() >= item.getLevel()) {
                stack.pop();
            }

            if (stack.isEmpty()) {
                result.add(item);
            } else {
                stack.peek().addChild(item);
            }

            stack.push(item);
        }

        return result;
    }

    /**
     * 提取文档标题
     */
    private static String extractTitle(List<String> lines) {
        Pattern h1Pattern = Pattern.compile("^#\\s+(.*)$");
        return lines.stream()
            .map(h1Pattern::matcher)
            .filter(Matcher::matches)
            .map(m -> m.group(1).trim())
            .findFirst()
            .orElse("Untitled Document");
    }

    /**
     * 补充元数据信息
     */
    private static void enrichWithMetadata(Path extractDir, MinerUParseResult result, long startTime) {
        MinerUMetadata metadata = new MinerUMetadata();

        try {
            // 查找 layout.json 文件
            Path layoutFile = Files.walk(extractDir)
                .filter(path -> path.getFileName().toString().equals("layout.json"))
                .findFirst()
                .orElse(null);

            if (layoutFile != null && Files.exists(layoutFile)) {
                String layoutContent = Files.readString(layoutFile, StandardCharsets.UTF_8);
                JsonNode layoutJson = objectMapper.readTree(layoutContent);
                metadata.setLayout(layoutJson);
                log.info("已加载 layout.json 元数据");
            }

            // 查找 content_list.json 文件
            Path contentListFile = Files.walk(extractDir)
                .filter(path -> path.getFileName().toString().contains("content_list.json"))
                .findFirst()
                .orElse(null);

            if (contentListFile != null && Files.exists(contentListFile)) {
                metadata.setFileSize(contentListFile.toFile().length());
                String contentListStr = Files.readString(contentListFile, StandardCharsets.UTF_8);
                JsonNode contentListJson = objectMapper.readTree(contentListStr);
                metadata.setContentList(contentListJson);

                // 尝试从文件名提取原始文件名
                String fileName = contentListFile.getFileName().toString();
                if (fileName.contains("_content_list.json")) {
                    String originalName = fileName.replace("_content_list.json", "");
                    metadata.setOriginalFilename(originalName);
                }

                log.info("已加载 content_list.json 元数据");
            }

            // 统计图片数量
            Path imagesDir = extractDir.resolve("images");
            if (Files.exists(imagesDir)) {
                long imageCount = Files.list(imagesDir).count();
                metadata.setImageCount((int) imageCount);
                log.info("找到 {} 张图片", imageCount);
            }

            // 设置处理完成时间
            metadata.markProcessingEnd(startTime);

        } catch (Exception e) {
            log.warn("读取元数据文件时出错", e);
        }

        result.setMetadata(metadata);
    }

    /**
     * 清理临时文件
     */
    private static void cleanupTempFiles(Path tempDir) {
        try {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.warn("删除临时文件失败: {}", path, e);
                    }
                });
            log.info("临时文件清理完成");
        } catch (IOException e) {
            log.warn("清理临时文件时出错", e);
        }
    }
}
