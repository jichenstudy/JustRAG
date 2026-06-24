package com.shujichen.rag.common.mineru.utils;

import com.google.protobuf.ServiceException;
import com.shujichen.rag.common.oss.core.OssClient;
import com.shujichen.rag.common.oss.entity.UploadResult;
import com.shujichen.rag.common.oss.factory.OssFactory;
import com.shujichen.rag.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 图片上传工具类
 */
@Slf4j
public class MarkdownImageUploader {

    private static final WebClient webClient = WebClient.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
        .build();

    /**
     * Markdown 图片引用的正则表达式
     * 匹配 ![alt](url) 和 <img src="url"> 格式
     */
    private static final Pattern IMAGE_PATTERN = Pattern.compile(
        "(?:" +
            "!\\[([^\\]]*)\\]\\(([^)]+)\\)" +  // ![alt](url) 格式
            "|" +
            "<img[^>]*?src\\s*=\\s*[\"']([^\"']+)[\"'][^>]*?/?>" +  // <img src="url"> 格式（更灵活）
        ")",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 处理 Markdown 内容，上传图片到 OSS 并替换 URL
     *
     * @param markdownContent Markdown 内容
     * @param baseDir        基础目录（用于解析相对路径图片）
     * @return 处理结果
     */
        public static ImageUploadResult processMarkdown(String markdownContent, String baseDir) {
        log.info("🔄 开始处理 Markdown 内容，基础目录: {}", baseDir);

        Map<String, UploadResult> uploadedImages = new HashMap<>();
        List<String> errors = new ArrayList<>();
        String processedContent = markdownContent;

        try {
            // 获取默认 OSS 客户端
            OssClient ossClient = OssFactory.instance();

            // 首先扫描所有图片引用
            Matcher scanMatcher = IMAGE_PATTERN.matcher(markdownContent);
            List<ImageReference> foundImages = new ArrayList<>();

            while (scanMatcher.find()) {
                String fullMatch = scanMatcher.group(0);
                String imageUrl = null;
                String altText = null;
                boolean isMarkdownFormat = false;

                // 判断是 ![alt](url) 还是 <img> 格式
                if (scanMatcher.group(2) != null) {
                    // ![alt](url) 格式
                    altText = scanMatcher.group(1);
                    imageUrl = scanMatcher.group(2);
                    isMarkdownFormat = true;
                } else if (scanMatcher.group(3) != null) {
                    // <img src="url"> 格式
                    imageUrl = scanMatcher.group(3);
                    isMarkdownFormat = false;
                }

                if (imageUrl != null) {
                    foundImages.add(new ImageReference(fullMatch, imageUrl, altText, isMarkdownFormat));
                }
            }

            log.info("🔍 扫描发现 {} 个图片引用", foundImages.size());

            // 分阶段处理：先上传唯一URL，再替换所有引用
            Set<String> processedUrls = new HashSet<>();
            int imageIndex = 1;

            // 第一阶段：上传唯一的图片URL
            for (ImageReference imgRef : foundImages) {
                // 跳过重复的URL（只上传一次）
                if (processedUrls.contains(imgRef.imageUrl)) {
                    continue;
                }

                processedUrls.add(imgRef.imageUrl);

                // 显示当前处理的图片信息
                String displayUrl = imgRef.imageUrl.length() > 60 ?
                    imgRef.imageUrl.substring(0, 57) + "..." : imgRef.imageUrl;
                log.info("📷 处理第{}张图片: {} ({})",
                    imageIndex++, displayUrl, imgRef.isMarkdownFormat ? "Markdown" : "HTML");

                try {
                    // 检查是否需要上传（跳过已经是OSS链接的图片）
                    if (isOssUrl(imgRef.imageUrl)) {
                        log.info("  ⏭️ 跳过OSS链接");
                        continue;
                    }

                    // 上传图片
                    UploadResult uploadResult = uploadImage(imgRef.imageUrl, baseDir, ossClient);
                    uploadedImages.put(imgRef.imageUrl, uploadResult);

                    log.info("  ✅ 上传成功: {}", uploadResult.getUrl());

                } catch (Exception e) {
                    String error = String.format("上传图片失败 [%s]: %s", imgRef.imageUrl, e.getMessage());
                    errors.add(error);
                    log.error("  ❌ {}", error);
                }
            }

            // 第二阶段：替换所有图片引用（包括重复的）
            log.info("🔄 开始替换图片引用...");
            int replaceIndex = 1;

            for (ImageReference imgRef : foundImages) {
                // 检查该URL是否有上传结果
                UploadResult uploadResult = uploadedImages.get(imgRef.imageUrl);
                if (uploadResult == null) {
                    // 没有上传结果，可能是OSS链接或上传失败
                    log.debug("  ⏭️ 第{}个引用跳过替换: {}", replaceIndex++, imgRef.imageUrl);
                    continue;
                }

                // 替换 URL
                String newUrl = uploadResult.getUrl();
                String displayUrl = imgRef.imageUrl.length() > 60 ?
                    imgRef.imageUrl.substring(0, 57) + "..." : imgRef.imageUrl;

                if (imgRef.isMarkdownFormat) {
                    String newImageRef = String.format("![%s](%s)",
                        imgRef.altText != null ? imgRef.altText : "", newUrl);
                    processedContent = processedContent.replace(imgRef.fullMatch, newImageRef);
                    log.info("  🔄 第{}个引用已替换: {} (Markdown)", replaceIndex++, displayUrl);
                } else {
                    processedContent = processedContent.replace(imgRef.imageUrl, newUrl);
                    log.info("  🔄 第{}个引用已替换: {} (HTML)", replaceIndex++, displayUrl);
                }
            }

        } catch (Exception e) {
            String error = "处理 Markdown 内容时发生错误: " + e.getMessage();
            errors.add(error);
            log.error("❌ {}", error, e);
        }

        ImageUploadResult result = new ImageUploadResult(markdownContent, processedContent, uploadedImages, errors);
        log.info("📊 处理完成: {}", result.getSummary());
        return result;
    }

    /**
     * 图片上传结果
     */
    public static class ImageUploadResult {
        private final String originalMarkdown;
        private final String processedMarkdown;
        private final Map<String, UploadResult> uploadedImages;
        private final List<String> errors;

        public ImageUploadResult(String originalMarkdown, String processedMarkdown,
                                 Map<String, UploadResult> uploadedImages, List<String> errors) {
            this.originalMarkdown = originalMarkdown;
            this.processedMarkdown = processedMarkdown;
            this.uploadedImages = uploadedImages;
            this.errors = errors;
        }

        public String getOriginalMarkdown() { return originalMarkdown; }
        public String getProcessedMarkdown() { return processedMarkdown; }
        public Map<String, UploadResult> getUploadedImages() { return uploadedImages; }
        public List<String> getErrors() { return errors; }

        public boolean hasErrors() { return !errors.isEmpty(); }
        public int getUploadedCount() { return uploadedImages.size(); }

        public String getSummary() {
            return String.format("处理完成：上传 %d 张图片，%d 个错误",
                    getUploadedCount(), errors.size());
        }
    }

    /**
     * 图片引用信息
     */
    private static class ImageReference {
        final String fullMatch;
        final String imageUrl;
        final String altText;
        final boolean isMarkdownFormat;

        ImageReference(String fullMatch, String imageUrl, String altText, boolean isMarkdownFormat) {
            this.fullMatch = fullMatch;
            this.imageUrl = imageUrl;
            this.altText = altText;
            this.isMarkdownFormat = isMarkdownFormat;
        }
    }

    /**
     * 上传单个图片
     *
     * @param imageUrl  图片 URL 或路径
     * @param baseDir   基础目录
     * @param ossClient OSS 客户端
     * @return 上传结果
     * @throws Exception 上传异常
     */
    private static UploadResult uploadImage(String imageUrl, String baseDir, OssClient ossClient) throws Exception {
        File imageFile = null;
        String originalFileName = getImageFileName(imageUrl);

        try {
            // 获取图片文件
            imageFile = getImageFile(imageUrl, baseDir);

            if (!imageFile.exists() || !imageFile.isFile()) {
                throw new ServiceException("图片文件不存在: " + imageUrl);
            }

            // 检查文件大小
            long fileSize = imageFile.length();
            if (fileSize == 0) {
                throw new ServiceException("图片文件为空: " + imageUrl);
            }

            if (fileSize > 20 * 1024 * 1024) { // 20MB 限制
                throw new ServiceException("图片文件过大: " + formatFileSize(fileSize));
            }

            // 检查文件类型
            String contentType = getContentType(imageFile);
            if (!isImageContentType(contentType)) {
                throw new ServiceException("不支持的图片格式: " + contentType);
            }

            // 获取文件后缀
            String suffix = getFileSuffix(originalFileName);

            // 直接上传到 OSS
            UploadResult uploadResult = ossClient.uploadSuffix(imageFile, suffix);

            if (uploadResult == null || StringUtils.isBlank(uploadResult.getUrl())) {
                throw new ServiceException("OSS 上传失败，返回结果为空");
            }

            return uploadResult;

        } finally {
            // 清理临时文件（仅清理从网络下载的临时文件）
            if (imageFile != null && isTemporaryFile(imageFile, imageUrl)) {
                try {
                    Files.deleteIfExists(imageFile.toPath());
                    log.debug("已清理临时文件: {}", imageFile.getPath());
                } catch (Exception e) {
                    log.warn("清理临时文件失败: {}", imageFile.getPath(), e);
                }
            }
        }
    }

    /**
     * 获取图片文件
     */
    private static File getImageFile(String imageUrl, String baseDir) throws Exception {
        // HTTP/HTTPS URL - 下载到临时文件
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return downloadImage(imageUrl);
        }

        // 本地文件路径
        Path imagePath;
        if (Paths.get(imageUrl).isAbsolute()) {
            // 绝对路径
            imagePath = Paths.get(imageUrl);
        } else {
            // 相对路径，结合基础目录
            if (StringUtils.isNotBlank(baseDir)) {
                imagePath = Paths.get(baseDir, imageUrl).normalize();
            } else {
                imagePath = Paths.get(imageUrl);
            }
        }

        return imagePath.toFile();
    }

    /**
     * 下载网络图片到临时文件
     */
    private static File downloadImage(String imageUrl) throws Exception {
        log.debug("正在下载图片: {}", imageUrl);

        byte[] imageData = webClient.get()
            .uri(imageUrl)
            .retrieve()
            .bodyToMono(byte[].class)
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
            .timeout(Duration.ofMinutes(2))
            .block();

        if (imageData == null || imageData.length == 0) {
            throw new ServiceException("下载图片失败或图片为空: " + imageUrl);
        }

        // 创建临时文件
        String fileName = getImageFileName(imageUrl);
        String suffix = getFileSuffix(fileName);

        File tempFile = File.createTempFile("mineru_img_", suffix);
        Files.write(tempFile.toPath(), imageData);

        log.debug("图片下载完成: {} -> {}", imageUrl, tempFile.getPath());
        return tempFile;
    }

    /**
     * 获取图片文件名
     */
    private static String getImageFileName(String imageUrl) {
        try {
            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                URI uri = new URI(imageUrl);
                String path = uri.getPath();
                if (StringUtils.isNotBlank(path)) {
                    String fileName = Paths.get(path).getFileName().toString();
                    if (StringUtils.isNotBlank(fileName) && fileName.contains(".")) {
                        return fileName;
                    }
                }
                return "image.jpg"; // 默认文件名
            } else {
                return Paths.get(imageUrl).getFileName().toString();
            }
        } catch (Exception e) {
            return "image.jpg";
        }
    }

    /**
     * 获取文件后缀
     */
    private static String getFileSuffix(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return ".jpg";
        }

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot);
        }

        return ".jpg";
    }

    /**
     * 获取文件内容类型
     */
    private static String getContentType(File file) throws IOException {
        String contentType = Files.probeContentType(file.toPath());
        if (StringUtils.isBlank(contentType)) {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                return "image/jpeg";
            } else if (fileName.endsWith(".png")) {
                return "image/png";
            } else if (fileName.endsWith(".gif")) {
                return "image/gif";
            } else if (fileName.endsWith(".webp")) {
                return "image/webp";
            } else if (fileName.endsWith(".bmp")) {
                return "image/bmp";
            } else {
                return "image/jpeg"; // 默认
            }
        }
        return contentType;
    }

    /**
     * 检查是否为图片内容类型
     */
    private static boolean isImageContentType(String contentType) {
        if (StringUtils.isBlank(contentType)) {
            return false;
        }

        String type = contentType.toLowerCase();
        return type.startsWith("image/") && (
            type.contains("jpeg") || type.contains("jpg") ||
            type.contains("png") || type.contains("gif") ||
            type.contains("webp") || type.contains("bmp")
        );
    }

    /**
     * 检查是否为 OSS URL
     */
    private static boolean isOssUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }

        // 可以根据实际的 OSS 域名配置来判断
        return url.contains(".oss-") ||
               url.contains(".cos.") ||
               url.contains(".qingstor.") ||
               url.contains("amazonaws.com") ||
               url.contains("aliyuncs.com") ||
               url.contains("localhost") ||
               url.contains("127.0.0.1");
    }

    /**
     * 判断是否为临时文件
     */
    private static boolean isTemporaryFile(File file, String originalUrl) {
        // 如果原始 URL 是网络地址，则认为是临时文件
        return originalUrl.startsWith("http://") || originalUrl.startsWith("https://");
    }

    /**
     * 格式化文件大小
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        }
        if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

}
