package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.connector.dto.ArtifactDeploymentStatus;
import com.onetuks.iflow_sentinel.connector.dto.TrackerArtifactResponse;
import com.onetuks.iflow_sentinel.connector.service.ArtifactExportService;
import com.onetuks.iflow_sentinel.connector.service.ArtifactTrackerService;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtifactExportServiceTest {

    @Mock
    private ArtifactTrackerService artifactTrackerService;

    @Test
    void exportsTrackerArtifactsAsXlsxRows() throws IOException {
        when(artifactTrackerService.list(1L)).thenReturn(List.of(
                new TrackerArtifactResponse("PKG1", "Package One", "ART1", "Art One", "1.0.0", "STARTED",
                        ArtifactDeploymentStatus.DEPLOYED),
                new TrackerArtifactResponse(null, null, "ART3", "Art Three", "1.0.0", "STARTED",
                        ArtifactDeploymentStatus.INACTIVE)));

        ArtifactExportService service = new ArtifactExportService(artifactTrackerService);
        byte[] content = service.export(1L);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheetAt(0);

            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Package");
            assertThat(header.getCell(4).getStringCellValue()).isEqualTo("Status");

            Row deployedRow = sheet.getRow(1);
            assertThat(deployedRow.getCell(0).getStringCellValue()).isEqualTo("Package One");
            assertThat(deployedRow.getCell(1).getStringCellValue()).isEqualTo("Art One");
            assertThat(deployedRow.getCell(4).getStringCellValue()).isEqualTo("DEPLOYED");

            Row inactiveRow = sheet.getRow(2);
            assertThat(inactiveRow.getCell(0).getStringCellValue()).isEqualTo("");
            assertThat(inactiveRow.getCell(4).getStringCellValue()).isEqualTo("INACTIVE");

            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(3);
        }
    }
}
