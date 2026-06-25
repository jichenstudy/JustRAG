package com.shujichen.rag.splitting;

import com.shujichen.rag.common.mineru.core.MinerUHttpService;
import com.shujichen.rag.common.mineru.entity.*;
import com.shujichen.rag.common.mineru.utils.MinerUResultParser;
import com.shujichen.rag.common.mineru.utils.MinerUUtils;
import com.shujichen.rag.common.oss.factory.OssFactory;
import com.shujichen.rag.common.util.StringUtils;
import com.shujichen.rag.entity.FileDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DocumentParser {

    @Autowired
    private MinerUHttpService minerUHttpService;

    /**
     * 解析文档内容
     * @param fileDetail 文件详情
     * @return 解析后的文本内容
     */
    public String parse(FileDetail fileDetail) throws Exception {
        String filename = fileDetail.getFilename();
        InputStream inputStream = OssFactory.instance().getObjectContent(fileDetail.getObjectName());
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        if (filename.endsWith(".docx")) {
            try (XWPFDocument doc = new XWPFDocument(inputStream)) {
                return doc.getParagraphs()
                        .stream()
                        .map(XWPFParagraph::getText)
                        .collect(Collectors.joining("\n"));
            }
        }

        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
                StringBuilder text = new StringBuilder();
                for (Sheet sheet : workbook) {
                    text.append("【Sheet: ").append(sheet.getSheetName()).append("】\n");
                    for (Row row : sheet) {
                        for (Cell cell : row) {
                            if (cell.getCellType() == CellType.STRING) {
                                text.append(cell.getStringCellValue()).append(" ");
                            } else if (cell.getCellType() == CellType.NUMERIC) {
                                text.append(cell.getNumericCellValue()).append(" ");
                            }
                        }
                        text.append("\n");
                    }
                }
                return text.toString();
            }
        }

        if (filename.endsWith(".doc")) {
            try (HWPFDocument doc = new HWPFDocument(inputStream)) {
                return doc.getDocumentText();
            }
        }

        if (filename.endsWith(".md") || filename.endsWith(".markdown")) {
//            try (BufferedReader reader = new BufferedReader(
//                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
//                return reader.lines().collect(Collectors.joining("\n"));
//            }

            return mineruParse(fileDetail);
        }

        if (filename.endsWith(".txt")) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }

        if (filename.endsWith(".html") || filename.endsWith(".htm")) {
            Document doc = Jsoup.parse(inputStream, StandardCharsets.UTF_8.name(), "");
            return doc.text();
        }

        if (filename.endsWith(".pdf")) {
//            try (PDDocument document = PDDocument.load(inputStream)) {
//                PDFTextStripper stripper = new PDFTextStripper();
//                return stripper.getText(document);
//            }

            return mineruParse(fileDetail);
        }

        throw new IllegalArgumentException("Unsupported file type: " + filename);
    }

    /**
     * MinerU 解析文档内容
     */
    private String mineruParse(FileDetail fileDetail) throws Exception {
        // 调用MinerU转化为markdown
        ExtractTaskRequest request = ExtractTaskRequest.builder()
                .url(fileDetail.getUrl())
                .isOcr(true)
                .enableFormula(true)
                .enableTable(true)
                .language("auto")
                .dataId(fileDetail.getId().toString())
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

            // 处理 MinerU 解析结果
            MinerUParseResult result = MinerUResultParser.processResult(data.getFullZipUrl());
            return result.getFullContent();
        }
        return StringUtils.EMPTY;
    }
}
