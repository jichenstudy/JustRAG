package com.shujichen.rag.splitting;

import com.shujichen.rag.common.enums.ChunkingMode;
import com.shujichen.rag.entity.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    /**
     * 根据策略切分文档
     *
     * @param documentId 文档ID
     * @param content    文档全文
     * @param strategy   策略：smart(结构感知) / fixed(固定长度)
     * @param chunkSize  分片最大字符数
     * @param overlap    重叠字符数（仅 fixed 策略生效）
     * @param minSize    最小分片字符数
     */
    public List<DocumentChunk> split(Long documentId, String content,
                                     String strategy, int chunkSize, int overlap, int minSize) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }

        int effectiveChunkSize = chunkSize > 0 ? chunkSize : 1000;
        int effectiveMinSize = Math.max(50, minSize);

        if (ChunkingMode.fromValue(strategy).isFixed()) {
            log.info("使用固定长度切分策略，documentId={}, chunkSize={}, overlap={}",
                    documentId, effectiveChunkSize, overlap);
            return splitFixed(documentId, content, effectiveChunkSize, Math.max(0, overlap), effectiveMinSize);
        }

        // 默认：结构感知
        log.info("使用结构感知切分策略，documentId={}, chunkSize={}, minSize={}",
                documentId, effectiveChunkSize, effectiveMinSize);
        return splitStructure(documentId, content, effectiveChunkSize, effectiveMinSize);
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
}
