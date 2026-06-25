package com.shujichen.rag.splitting;

import cn.hutool.core.util.StrUtil;
import com.shujichen.rag.common.enums.ChunkingMode;
import com.shujichen.rag.common.mineru.utils.MarkdownImageUploader;
import com.shujichen.rag.entity.AiModelConfig;
import com.shujichen.rag.entity.DocumentChunk;
import com.shujichen.rag.entity.KnowledgeBase;
import com.shujichen.rag.factory.ChatClientFactory;
import com.shujichen.rag.service.AiModelConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档智能分片服务
 */
@Service
@Slf4j
public class MarkdownSplittingService {

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$");

    @Autowired
    private ChatClientFactory chatClientFactory;

    @Autowired
    private AiModelConfigService aiModelConfigService;

    /**
     * 根据策略切分文档
     *
     * @param knowledgeBase 知识库
     * @param documentId 文档ID
     * @param content    文档全文
     * @param strategy   策略：smart(结构感知) / fixed(固定长度)
     * @param chunkSize  分片最大字符数
     * @param overlap    重叠字符数（仅 fixed 策略生效）
     * @param minSize    最小分片字符数
     */
    public List<DocumentChunk> split(KnowledgeBase knowledgeBase, Long documentId, String content,
                                     String strategy, int chunkSize, int overlap, int minSize) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }

        List<DocumentChunk> chunks;
        int effectiveChunkSize = chunkSize > 0 ? chunkSize : 1000;
        int effectiveMinSize = Math.max(50, minSize);

        if (ChunkingMode.fromValue(strategy).isFixed()) {
            log.info("使用固定长度切分策略，documentId={}, chunkSize={}, overlap={}",
                    documentId, effectiveChunkSize, overlap);
            chunks = splitFixed(documentId, content, effectiveChunkSize, Math.max(0, overlap), effectiveMinSize);
        } else {
            // 默认：结构感知
            log.info("使用结构感知切分策略，documentId={}, chunkSize={}, minSize={}",
                    documentId, effectiveChunkSize, effectiveMinSize);
            chunks = splitStructure(documentId, content, effectiveChunkSize, effectiveMinSize);
        }

        // 切分后处理分片多模态内容
        processChunkMultimodal(knowledgeBase, chunks);

        return chunks;
    }

    // ==================== 策略一：结构感知（无重叠） ====================

    /**
     * 结构感知切分：
     * 1. 解析 Markdown 标题 → 章节树
     * 2. 每个章节作为一个候选分片
     * 3. 大章节按段落边界拆分
     * 4. 超长段落按句子边界拆分
     * 5. 超长句子按字符拆分（最终兜底）
     * 注意：此策略不使用重叠窗口
     */
    private List<DocumentChunk> splitStructure(Long documentId, String text,
                                                     int chunkSize, int minSize) {
        // 尝试解析 Markdown 章节结构
        List<Section> sections = parseSections(text);

        if (sections.isEmpty()) {
            // 无章节结构 → 直接按段落切分
            log.debug("无 Markdown 章节结构，按段落切分, documentId={}", documentId);
            List<ChunkWithPos> raw = splitByParagraphs(
                    documentId, text, 0, text.length(), null, chunkSize, minSize, 0);
            return toChunkList(documentId, raw, null, null);
        }

        // 按章节递归切分
        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;
        for (Section section : sections) {
            chunkIndex = processSection(documentId, section, "", chunks, chunkIndex,
                    text, chunkSize, minSize);
        }
        return chunks;
    }

    /**
     * 递归处理章节
     */
    private int processSection(Long documentId, Section section, String parentPath,
                                List<DocumentChunk> chunks, int chunkIndex,
                                String fullText, int chunkSize, int minSize) {
        String path = parentPath.isEmpty()
                ? section.title
                : parentPath + " > " + section.title;

        // 章节完整内容 = 标题行 + 正文
        String sectionFullText = section.headingLine + "\n\n" + section.content;
        int secStart = section.startIndex;
        int secEnd = section.endIndex;

        if (sectionFullText.length() <= chunkSize) {
            // 章节内容可直接作为一个分片
            if (sectionFullText.trim().length() >= minSize) {
                chunks.add(buildChunk(documentId, chunkIndex++, sectionFullText.trim(),
                        path, section.title, chunkIndex - 1, secStart, secEnd));
            }
        } else {
            // 章节内容太大 → 按段落拆分
            List<ChunkWithPos> subChunks = splitByParagraphs(
                    documentId, sectionFullText,
                    secStart, secEnd, path,
                    chunkSize, minSize, chunkIndex);
            for (ChunkWithPos sc : subChunks) {
                chunks.add(buildChunk(documentId, chunkIndex++, sc.content,
                        path, section.title, chunkIndex - 1, sc.start, sc.end));
            }
        }

        // 递归处理子章节（子章节是独立的章节，不并入父章节）
        for (Section child : section.children) {
            chunkIndex = processSection(documentId, child, path, chunks, chunkIndex,
                    fullText, chunkSize, minSize);
        }

        return chunkIndex;
    }

    /**
     * 按段落拆分一段文本（结构感知的降级手段）
     * 按双换行分割段落，合并到接近 chunkSize 时输出
     */
    private List<ChunkWithPos> splitByParagraphs(Long documentId, String text,
                                                  int baseStart, int baseEnd,
                                                  String sectionPath,
                                                  int chunkSize, int minSize,
                                                  int startChunkIndex) {
        List<ChunkWithPos> results = new ArrayList<>();
        String[] paragraphs = text.split("\n\n+");
        if (paragraphs.length == 0) {
            return results;
        }

        StringBuilder buffer = new StringBuilder();
        int bufferStart = baseStart;
        int cursor = baseStart;

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int paraStart = text.indexOf(trimmed, Math.max(0, cursor - baseStart)) + baseStart;
            if (paraStart < baseStart) {
                paraStart = cursor;
            }
            int paraEnd = paraStart + trimmed.length();

            // 单个段落就超过 chunkSize → 按句子拆分
            if (trimmed.length() > chunkSize) {
                // 先输出缓冲区
                if (buffer.length() >= minSize) {
                    results.add(new ChunkWithPos(buffer.toString().trim(), bufferStart, cursor));
                }
                buffer.setLength(0);

                // 按句子拆分这个超长段落
                List<ChunkWithPos> sentenceChunks = splitBySentences(
                        trimmed, paraStart, chunkSize, minSize);
                results.addAll(sentenceChunks);

                cursor = paraEnd + 2;
                bufferStart = cursor;
                continue;
            }

            // 当前段落加入后会超限 → 输出缓冲区
            if (buffer.length() > 0 && buffer.length() + trimmed.length() + 2 > chunkSize) {
                if (buffer.length() >= minSize) {
                    results.add(new ChunkWithPos(buffer.toString().trim(), bufferStart, cursor));
                }
                buffer.setLength(0);
                bufferStart = paraStart;
            }

            if (buffer.length() > 0) {
                buffer.append("\n\n");
            }
            buffer.append(trimmed);
            cursor = paraEnd + 2;
        }

        // 最后残留
        if (buffer.length() >= minSize) {
            results.add(new ChunkWithPos(buffer.toString().trim(), bufferStart, baseEnd));
        }

        return results;
    }

    /**
     * 按句子拆分（超长段落的降级）
     */
    private List<ChunkWithPos> splitBySentences(String text, int baseOffset,
                                                 int chunkSize, int minSize) {
        List<ChunkWithPos> results = new ArrayList<>();
        // 找句子边界
        List<Integer> boundaries = new ArrayList<>();
        boundaries.add(0);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '；'
                    || c == '.' || c == '!' || c == '?' || c == '\n') {
                boundaries.add(i + 1);
            }
        }
        if (boundaries.get(boundaries.size() - 1) < text.length()) {
            boundaries.add(text.length());
        }

        StringBuilder buffer = new StringBuilder();
        int bufferStart = baseOffset;
        int cursor = baseOffset;

        for (int i = 1; i < boundaries.size(); i++) {
            int sentStart = boundaries.get(i - 1);
            int sentEnd = boundaries.get(i);
            String sentence = text.substring(sentStart, sentEnd);
            if (sentence.trim().isEmpty()) {
                cursor = baseOffset + sentEnd;
                continue;
            }

            // 单句就超长 → 按字符拆分（最终兜底）
            if (sentence.length() > chunkSize) {
                if (buffer.length() >= minSize) {
                    results.add(new ChunkWithPos(buffer.toString().trim(), bufferStart, cursor));
                }
                buffer.setLength(0);

                List<ChunkWithPos> charChunks = splitByCharacters(
                        sentence, baseOffset + sentStart, chunkSize);
                results.addAll(charChunks);

                cursor = baseOffset + sentEnd;
                bufferStart = cursor;
                continue;
            }

            if (buffer.length() > 0 && buffer.length() + sentence.length() > chunkSize) {
                if (buffer.length() >= minSize) {
                    results.add(new ChunkWithPos(buffer.toString().trim(), bufferStart, cursor));
                }
                buffer.setLength(0);
                bufferStart = baseOffset + sentStart;
            }

            buffer.append(sentence);
            cursor = baseOffset + sentEnd;
        }

        if (buffer.length() >= minSize) {
            results.add(new ChunkWithPos(buffer.toString().trim(), bufferStart, baseOffset + text.length()));
        }

        return results;
    }

    /**
     * 按字符拆分（最终兜底，超长句子无处可退时使用）
     */
    private List<ChunkWithPos> splitByCharacters(String text, int baseOffset, int chunkSize) {
        List<ChunkWithPos> results = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            String part = text.substring(i, end).trim();
            if (!part.isEmpty()) {
                results.add(new ChunkWithPos(part, baseOffset + i, baseOffset + end));
            }
        }
        return results;
    }

    // ==================== 策略二：固定长度（有重叠） ====================

    /**
     * 固定长度切分：滑动窗口 + 重叠
     */
    private List<DocumentChunk> splitFixed(Long documentId, String text,
                                            int chunkSize, int overlap, int minSize) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int step = Math.max(1, chunkSize - overlap); // 步长 = chunkSize - overlap

        int index = 0;
        for (int i = 0; i < text.length(); i += step) {
            int end = Math.min(i + chunkSize, text.length());
            String content = text.substring(i, end).trim();
            if (content.length() >= minSize) {
                chunks.add(buildChunk(documentId, index++, content,
                        null, null, index - 1, i, end));
            }
        }
        return chunks;
    }

    // ==================== Markdown 章节解析 ====================

    /**
     * 解析 Markdown 标题结构，返回根章节列表
     */
    private List<Section> parseSections(String text) {
        String[] lines = text.split("\n", -1);
        List<Section> roots = new ArrayList<>();
        Deque<Section> stack = new ArrayDeque<>();

        Section current = null;
        StringBuilder contentBuf = new StringBuilder();
        int charOffset = 0;
        boolean hasAnyHeading = false;

        for (String line : lines) {
            Matcher m = HEADING_PATTERN.matcher(line);
            int lineLen = line.length() + 1;

            if (m.matches()) {
                hasAnyHeading = true;
                // 保存前一个章节内容
                if (current != null) {
                    current.content = contentBuf.toString().trim();
                    current.endIndex = charOffset;
                    contentBuf = new StringBuilder();
                }

                int level = m.group(1).length();
                String title = m.group(2).trim();

                Section sec = new Section();
                sec.level = level;
                sec.title = title;
                sec.headingLine = line;
                sec.startIndex = charOffset;

                // 维护层级栈
                while (!stack.isEmpty() && stack.peek().level >= level) {
                    stack.pop();
                }
                if (stack.isEmpty()) {
                    roots.add(sec);
                } else {
                    stack.peek().children.add(sec);
                }
                stack.push(sec);
                current = sec;
            } else {
                contentBuf.append(line).append("\n");
            }
            charOffset += lineLen;
        }

        // 最后一个章节
        if (current != null) {
            current.content = contentBuf.toString().trim();
            current.endIndex = text.length();
        }

        if (!hasAnyHeading) {
            return Collections.emptyList();
        }

        return roots;
    }

    // ==================== 工具方法 ====================

    /**
     * 将 ChunkWithPos 列表转为 DocumentChunk 列表（用于无章节结构的降级场景）
     */
    private List<DocumentChunk> toChunkList(Long documentId, List<ChunkWithPos> raw,
                                             String sectionPath, String sectionTitle) {
        List<DocumentChunk> result = new ArrayList<>();
        for (int i = 0; i < raw.size(); i++) {
            ChunkWithPos c = raw.get(i);
            result.add(buildChunk(documentId, i, c.content,
                    sectionPath, sectionTitle, i, c.start, c.end));
        }
        return result;
    }

    private DocumentChunk buildChunk(Long documentId, int index, String content,
                                      String sectionPath, String sectionTitle,
                                      int position, int charStart, int charEnd) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(documentId);
        chunk.setChunkIndex(index);
        chunk.setContent(content);
        chunk.setTokenSize(content.length());
        chunk.setSectionPath(sectionPath);
        chunk.setSectionTitle(sectionTitle);
        chunk.setPosition(position);
        chunk.setCharStartIndex(charStart);
        chunk.setCharEndIndex(charEnd);
        chunk.setCreatedAt(LocalDateTime.now());
        return chunk;
    }

    // ==================== 内部类型 ====================

    /**
     * 章节节点
     */
    private static class Section {
        int level;
        String title;
        String headingLine;
        String content;
        int startIndex;
        int endIndex;
        List<Section> children = new ArrayList<>();
    }

    /**
     * 带位置的分片片段
     */
    private static class ChunkWithPos {
        String content;
        int start;
        int end;

        ChunkWithPos(String content, int start, int end) {
            this.content = content;
            this.start = start;
            this.end = end;
        }
    }

    /**
     * 切分后处理分片多模态内容
     */
    private void processChunkMultimodal(KnowledgeBase knowledgeBase, List<DocumentChunk> chunks) {
        // 获取视觉模型
        AiModelConfig visionModelConfig = null;
        if (knowledgeBase != null && knowledgeBase.getVisionModelId() != null) {
            visionModelConfig = aiModelConfigService.getModelConfigById(knowledgeBase.getVisionModelId());
        }
        if (visionModelConfig == null) {
            log.info("视觉模型配置不存在，跳过分片多模态处理");
            return;
        }
        // 创建 ChatClient
        ChatClient chatClient = chatClientFactory.createChatClient(visionModelConfig, null);

        for (DocumentChunk chunk : chunks) {
            // 获取分片媒体资源
            String chunkContent = chunk.getContent();
            List<String> imageUrls = extractImageUrls(chunkContent);
            if (imageUrls.isEmpty()) {
                continue;
            }

            // 批量视觉理解
            Map<String, String> resultMap = parseBatchVisionResult(chatClient, chunkContent, imageUrls);
            if (resultMap.isEmpty()) {
                log.warn("分片章节: {}, 视觉理解结果为空，跳过替换", chunk.getSectionPath());
                continue;
            }

            // 替换原切片中的图片描述：![alt](url) → ![视觉描述](url)
            String newContent = replaceImageDescriptions(chunkContent, resultMap);
            chunk.setContent(newContent);
            chunk.setTokenSize(newContent.length());
            log.info("分片章节: {}, 已替换 {} 张图片的描述", chunk.getSectionPath(), resultMap.size());
        }
    }

    /**
     * 将分片内容中的图片引用替换为视觉理解描述
     * <p>
     * 支持两种格式：
     * <ul>
     *   <li>{@code ![alt](url)} → {@code ![视觉描述](url)}</li>
     *   <li>{@code <img src="url">} → {@code <img src="url" alt="视觉描述"/>}</li>
     * </ul>
     *
     * @param content         原始分片内容
     * @param descriptionMap  key=图片URL, value=视觉理解描述
     * @return 替换后的内容
     */
    private String replaceImageDescriptions(String content, Map<String, String> descriptionMap) {
        if (descriptionMap.isEmpty() || StrUtil.isBlank(content)) {
            return content;
        }

        String result = content;
        String q = "\"";  // 双引号字符，避免字符串内 \" 转义问题
        for (Map.Entry<String, String> entry : descriptionMap.entrySet()) {
            String imageUrl = entry.getKey();
            String description = entry.getValue();
            String urlQuoted = Pattern.quote(imageUrl);

            // 1. 替换 Markdown 格式: ![alt](url) → ![description](url)
            Pattern mdPattern = Pattern.compile("!\\[([^\\]]*)\\]\\(" + urlQuoted + "\\)");
            Matcher mdMatcher = mdPattern.matcher(result);
            if (mdMatcher.find()) {
                String markdownRef = "![" + Matcher.quoteReplacement(description) + "](" + imageUrl + ")";
                result = mdMatcher.replaceAll(markdownRef);
            }

            // 2. 替换 <img> 标签格式（先移除旧alt，再添加视觉描述）
            //    匹配: <img ... src="url" ... > 或 <img ... src='url' ... >
            Pattern imgFindPattern = Pattern.compile(
                    "<img([^>]*?)src\\s*=\\s*[\"']" + urlQuoted + "[\"']([^>]*?)/?>",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher imgMatcher = imgFindPattern.matcher(result);
            if (imgMatcher.find()) {
                imgMatcher.reset();
                StringBuffer sb = new StringBuffer();
                while (imgMatcher.find()) {
                    String beforeSrc = imgMatcher.group(1);  // src 之前的所有属性
                    String afterSrc = imgMatcher.group(2);   // src 之后的所有属性
                    // 移除已有的 alt 属性（如果有）
                    String cleanedAfter = afterSrc.replaceAll("\\s+alt\\s*=\\s*[\"'][^\"']*[\"']", "");
                    // 构建新标签，使用 Matcher.quoteReplacement 防止 description 中的特殊字符
                    String newImgTag = "<img" + beforeSrc + "src=" + q + imageUrl + q
                            + " alt=" + q + Matcher.quoteReplacement(description) + q
                            + cleanedAfter + "/>";
                    imgMatcher.appendReplacement(sb, newImgTag);
                }
                imgMatcher.appendTail(sb);
                result = sb.toString();
            }
        }
        return result;
    }

    /**
     * 解析批量视觉理解返回的文本，提取为 Map<图片URL, 解析知识>
     *
     * @param chatClient  AI客户端
     * @param chunkContent  分片上下文内容
     * @param imageUrls 有效的图片URL列表（保持原始顺序）
     * @return Map，key 为图片URL，value 为解析知识
     */
    private Map<String, String> parseBatchVisionResult(ChatClient chatClient, String chunkContent, List<String> imageUrls) {
        Map<String, String> resultMap = new LinkedHashMap<>();

        // 构建所有图片的 Media 列表
        List<Media> mediaList = new ArrayList<>();
        List<String> validUrls = new ArrayList<>();
        for (String url : imageUrls) {
            try {
                Optional<MediaType> mediaType = MediaTypeFactory.getMediaType(url);
                Media media = new Media(mediaType.orElse(MediaType.IMAGE_JPEG), new URI(url));
                mediaList.add(media);
                validUrls.add(url);
            } catch (Exception e) {
                log.error("图片加载失败，已跳过: {}", url, e);
            }
        }

        if (mediaList.isEmpty()) {
            return resultMap;
        }

        // 构建提示词
        String prompt = buildBatchVisionPrompt(chunkContent, validUrls.size());
        UserMessage userMessage = new UserMessage(prompt);
        userMessage.getMedia().addAll(mediaList);

        // 调用视觉模型
        try {
            ChatResponse response = chatClient.prompt()
                    .messages(userMessage)
                    .call()
                    .chatResponse();

            if (response != null && response.getResult() != null) {
                String rawText = response.getResult().getOutput().getText();
                log.info("=== AI 原始返回 ===\n{}\n====================", rawText);

                // 解析返回结果为 Map<图片URL, 解析知识>
                Pattern pattern = Pattern.compile(
                        "===IMG_(\\d+)===\\s*([\\s\\S]*?)\\s*===IMG_END===",
                        Pattern.MULTILINE
                );
                Matcher matcher = pattern.matcher(rawText);

                while (matcher.find()) {
                    int index = Integer.parseInt(matcher.group(1)) - 1;
                    String knowledge = matcher.group(2).trim();

                    if (index >= 0 && index < imageUrls.size()) {
                        resultMap.put(imageUrls.get(index), knowledge);
                    }
                }
            }
        } catch (Exception e) {
            log.error("批量视觉理解失败", e);
        }

        return resultMap;
    }


    /**
     * 提取中所有的图片URL
     *
     * @param content 内容
     * @return 图片URL列表
     */
    private List<String> extractImageUrls(String content) {
        List<String> imageUrls = new ArrayList<>();
        if (StrUtil.isBlank(content)) {
            return imageUrls;
        }

        // 匹配所有图片引用
        Pattern pattern = MarkdownImageUploader.IMAGE_PATTERN;
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            // group(2): ![alt](url) 格式的URL
            // group(3): <img src="url"> 格式的URL
            String url = matcher.group(2) != null
                    ? matcher.group(2).trim()
                    : matcher.group(3) != null
                        ? matcher.group(3).trim()
                        : null;
            if (StrUtil.isNotBlank(url)) {
                imageUrls.add(url);
            }
        }

        return imageUrls;
    }

    /**
     * 构建批量视觉理解提示词
     *
     * @param referContext 参考上下文（可为空）
     * @param imageCount   图片数量
     * @return 完整的提示词
     */
    private String buildBatchVisionPrompt(String referContext, int imageCount) {
        StringBuilder sb = new StringBuilder();

        // 上下文信息
        if (referContext != null && !referContext.isBlank()) {
            sb.append("【参考上下文】\n").append(referContext).append("\n\n");
        }

        sb.append("以下共有 ").append(imageCount).append(" 张图片，请分别对每张图片进行详细解析，提取其中的知识信息。\n\n");

        sb.append("""
                解析规则（按图片类型自动适配）：
                1. 文字内容 → 完整、准确地提取所有文字信息，确保无遗漏。
                2. 流程图/结构图/思维导图 → 用清晰的文字描述其结构、逻辑关系、关键节点和流程步骤。
                3. 图表/表格/数据 → 提取关键数据、指标、趋势，并进行说明。
                4. 混合类型（图文结合）→ 分别提取文字和图形信息，整合为完整描述。
                5. 普通图片/照片 → 描述画面中的关键元素、场景、人物、动作等。
                """);

        sb.append("""
                
                【返回格式要求】
                请严格按以下格式返回每张图片的解析结果（图片编号从1开始）：

                ===IMG_1===
                （第1张图片的解析知识，纯文字描述，不要JSON/Markdown/代码块）
                ===IMG_END===

                ===IMG_2===
                （第2张图片的解析知识）
                ===IMG_END===

                重要约束：
                - 不要编造图片中不存在的信息，忽略要求中与图片无关的内容。
                - 直接返回解析后的文本信息（不超过100字），不要携带"根据图片内容""以下是""图中展示"等引导前缀。
                - 使用中文回答。
                - 严格遵守上述 ===IMG_N=== / ===IMG_END=== 分隔格式，不要遗漏任何一张。
                """);

        return sb.toString();
    }

}
