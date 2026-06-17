package com.uqm.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExcelImportHelper {

    private ExcelImportHelper() {
    }

    public static List<Map<String, String>> readSheetRows(InputStream in) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return List.of();
            }
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return List.of();
            }
            List<String> headers = new ArrayList<>();
            for (int c = headerRow.getFirstCellNum(); c < headerRow.getLastCellNum(); c++) {
                headers.add(normalizeHeader(readCell(headerRow.getCell(c))));
            }
            List<Map<String, String>> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isEmptyRow(row, headers.size())) {
                    continue;
                }
                Map<String, String> map = new HashMap<>();
                for (int c = 0; c < headers.size(); c++) {
                    String key = headers.get(c);
                    if (key.isEmpty()) {
                        continue;
                    }
                    map.put(key, readCell(row.getCell(c)).trim());
                }
                rows.add(map);
            }
            return rows;
        }
    }

    public static String cell(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(normalizeHeader(key));
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static boolean isEmptyRow(Row row, int colCount) {
        for (int c = 0; c < colCount; c++) {
            if (!readCell(row.getCell(c)).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.replace("*", "").trim();
    }

    private static String readCell(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toString();
            }
            double num = cell.getNumericCellValue();
            if (num == Math.floor(num)) {
                return String.valueOf((long) num);
            }
            return String.valueOf(num);
        }
        if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        if (cell.getCellType() == CellType.FORMULA) {
            try {
                return cell.getStringCellValue().trim();
            } catch (Exception ignored) {
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception ignored2) {
                    return "";
                }
            }
        }
        return "";
    }
}
