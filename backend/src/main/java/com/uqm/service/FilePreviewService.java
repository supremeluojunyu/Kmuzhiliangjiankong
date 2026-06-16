package com.uqm.service;

import com.uqm.common.BusinessException;
import com.uqm.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FilePreviewService {

    private static final Set<String> OFFICE_EXT = Set.of(
            "doc", "docx", "xls", "xlsx", "ppt", "pptx");
    private static final int MAX_EXCEL_ROWS = 200;
    private static final int MAX_EXCEL_COLS = 30;

    private final StorageProvider storageProvider;

    public String toPreviewHtml(String fileName) {
        String safe = StringUtils.cleanPath(fileName);
        if (safe.contains("..")) {
            throw new BusinessException(400, "非法文件名");
        }
        String ext = extractExt(safe).toLowerCase(Locale.ROOT);
        if (!OFFICE_EXT.contains(ext)) {
            throw new BusinessException(400, "该文件类型不支持 HTML 预览");
        }
        try (InputStream in = storageProvider.load(safe).getInputStream()) {
            String body = switch (ext) {
                case "docx" -> previewDocx(in);
                case "doc" -> previewDoc(in);
                case "xlsx", "xls" -> previewExcel(in);
                case "pptx" -> previewPptx(in);
                case "ppt" -> previewPpt(in);
                default -> throw new BusinessException(400, "不支持的预览格式");
            };
            return wrapHtml(body, safe);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "文档预览失败：" + e.getMessage());
        }
    }

    private String previewDocx(InputStream in) throws Exception {
        StringBuilder html = new StringBuilder();
        try (XWPFDocument doc = new XWPFDocument(in)) {
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                appendParagraph(html, paragraph.getText());
            }
            for (XWPFTable table : doc.getTables()) {
                html.append("<table>");
                for (XWPFTableRow row : table.getRows()) {
                    html.append("<tr>");
                    for (XWPFTableCell cell : row.getTableCells()) {
                        html.append("<td>").append(escape(cell.getText())).append("</td>");
                    }
                    html.append("</tr>");
                }
                html.append("</table>");
            }
        }
        return html.toString();
    }

    private String previewDoc(InputStream in) throws Exception {
        StringBuilder html = new StringBuilder();
        try (HWPFDocument doc = new HWPFDocument(in);
             WordExtractor extractor = new WordExtractor(doc)) {
            for (String paragraph : extractor.getParagraphText()) {
                appendParagraph(html, paragraph);
            }
        }
        return html.toString();
    }

    private String previewExcel(InputStream in) throws Exception {
        StringBuilder html = new StringBuilder();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                html.append("<h3>").append(escape(sheet.getSheetName())).append("</h3>");
                html.append("<table>");
                int rows = Math.min(sheet.getLastRowNum() + 1, MAX_EXCEL_ROWS);
                for (int r = 0; r < rows; r++) {
                    Row row = sheet.getRow(r);
                    html.append("<tr>");
                    int cols = 0;
                    if (row != null && row.getLastCellNum() > 0) {
                        cols = Math.min(row.getLastCellNum(), MAX_EXCEL_COLS);
                    }
                    for (int c = 0; c < cols; c++) {
                        Cell cell = row.getCell(c);
                        html.append("<td>").append(escape(formatCell(cell))).append("</td>");
                    }
                    html.append("</tr>");
                }
                html.append("</table>");
                if (sheet.getLastRowNum() + 1 > MAX_EXCEL_ROWS) {
                    html.append("<p class=\"hint\">仅展示前 ").append(MAX_EXCEL_ROWS).append(" 行</p>");
                }
            }
        }
        return html.toString();
    }

    private String previewPptx(InputStream in) throws Exception {
        StringBuilder html = new StringBuilder();
        try (XMLSlideShow ppt = new XMLSlideShow(in)) {
            int index = 1;
            for (XSLFSlide slide : ppt.getSlides()) {
                html.append("<div class=\"slide\"><h4>幻灯片 ").append(index++).append("</h4>");
                appendSlideShapes(html, slide.getShapes());
                html.append("</div>");
            }
        }
        return html.toString();
    }

    private String previewPpt(InputStream in) throws Exception {
        StringBuilder html = new StringBuilder();
        try (HSLFSlideShow ppt = new HSLFSlideShow(in)) {
            int index = 1;
            for (HSLFSlide slide : ppt.getSlides()) {
                html.append("<div class=\"slide\"><h4>幻灯片 ").append(index++).append("</h4>");
                for (org.apache.poi.hslf.usermodel.HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape textShape) {
                        appendParagraph(html, textShape.getText());
                    }
                }
                html.append("</div>");
            }
        }
        return html.toString();
    }

    private void appendSlideShapes(StringBuilder html, Iterable<XSLFShape> shapes) {
        for (XSLFShape shape : shapes) {
            if (shape instanceof XSLFTextShape textShape) {
                appendParagraph(html, textShape.getText());
            }
        }
    }

    private void appendParagraph(StringBuilder html, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        html.append("<p>").append(escape(text.trim())).append("</p>");
    }

    private String formatCell(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toString()
                    : String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private String escape(String text) {
        return text == null ? "" : HtmlUtils.htmlEscape(text);
    }

    private String wrapHtml(String body, String fileName) {
        if (!StringUtils.hasText(body)) {
            body = "<p>（文档无文本内容）</p>";
        }
        return """
                <!DOCTYPE html><html><head><meta charset="UTF-8"/>
                <style>
                  body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;padding:16px;color:#333;line-height:1.6;}
                  table{border-collapse:collapse;width:100%%;margin:12px 0;font-size:13px;}
                  td,th{border:1px solid #d9d9d9;padding:6px 8px;vertical-align:top;}
                  h3{margin:16px 0 8px;font-size:15px;}
                  h4{margin:0 0 8px;font-size:14px;color:#555;}
                  .slide{border:1px solid #eee;border-radius:6px;padding:12px;margin-bottom:12px;background:#fafafa;}
                  .hint{color:#999;font-size:12px;}
                  p{margin:0 0 8px;white-space:pre-wrap;word-break:break-word;}
                </style></head><body>
                <h2 style="font-size:16px;margin-top:0;">%s</h2>
                %s
                </body></html>
                """.formatted(escape(fileName), body);
    }

    private String extractExt(String name) {
        int idx = name.lastIndexOf('.');
        return idx >= 0 ? name.substring(idx + 1) : "";
    }
}
