package com.onetuks.iflow_sentinel.connector.service;

import com.onetuks.iflow_sentinel.connector.dto.TrackerArtifactResponse;
import com.onetuks.iflow_sentinel.exception.ConnectorException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/** 아티팩트 추적 목록을 xlsx 워크북으로 변환한다. */
@Service
public class ArtifactExportService {

    private static final String[] HEADERS = {"Package", "Artifact", "Version", "Runtime Status", "Status"};

    private final ArtifactTrackerService artifactTrackerService;

    public ArtifactExportService(ArtifactTrackerService artifactTrackerService) {
        this.artifactTrackerService = artifactTrackerService;
    }

    public byte[] export(Long tenantId) {
        List<TrackerArtifactResponse> artifacts = artifactTrackerService.list(tenantId);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Artifacts");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (TrackerArtifactResponse artifact : artifacts) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(nullToEmpty(artifact.packageName()));
                row.createCell(1).setCellValue(nullToEmpty(artifact.artifactName()));
                row.createCell(2).setCellValue(nullToEmpty(artifact.version()));
                row.createCell(3).setCellValue(nullToEmpty(artifact.runtimeStatus()));
                row.createCell(4).setCellValue(artifact.status().name());
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ConnectorException("Excel 파일 생성에 실패했습니다.", 500, e);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
