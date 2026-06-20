package com.shujichen.rag.splitting;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class DocumentParser {

    /**
     * 解析文档内容
     * @param inputStream 文件输入流
     * @param filename 文件名（用于判断文件类型）
     * @return 解析后的文本内容
     */
    public static String parse(InputStream inputStream, String filename) throws Exception {
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
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }

            // todo 智能解析
            // 处理相关资源文件, 然后返回md内容
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
            try (PDDocument document = PDDocument.load(inputStream)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }

            // todo 智能解析
            // 先转换为md, 再处理相关资源文件, 然后返回md内容
        }

        throw new IllegalArgumentException("Unsupported file type: " + filename);
    }

    /**
     * 解析文档内容（兼容 MultipartFile）
     * @param file 上传的文件
     * @return 解析后的文本内容
     */
    public static String parse(MultipartFile file) throws Exception {
        return parse(file.getInputStream(), file.getOriginalFilename());
    }
}
