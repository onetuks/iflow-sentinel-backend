package com.onetuks.iflow_sentinel.parser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParserFacadeErrorHandlingTest {

    private final ParserFacade parserFacade = new ParserFacade();

    @Test
    void notAZipThrowsParserException() {
        byte[] garbage = "this is not a zip file".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> parserFacade.parse(garbage))
                .isInstanceOf(ParserException.class);
    }

    @Test
    void zipWithoutIflwThrowsParserException() {
        byte[] zipWithOnlyManifest = zipOf(entry("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n\n"));

        assertThatThrownBy(() -> parserFacade.parse(zipWithOnlyManifest))
                .isInstanceOf(ParserException.class);
    }

    @Test
    void malformedIflwXmlThrowsParserException() {
        byte[] zipWithBrokenIflw = zipOf(
                entry("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\nBundle-Name: X\nBundle-Version: 1.0\n\n"),
                entry("scenarioflows/integrationflow/broken.iflw", "<bpmn2:definitions><unterminated>")
        );

        assertThatThrownBy(() -> parserFacade.parse(zipWithBrokenIflw))
                .isInstanceOf(ParserException.class);
    }

    private static byte[] zipOf(Entry... entries) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Entry entry : entries) {
                zos.putNextEntry(new ZipEntry(entry.name));
                zos.write(entry.content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static Entry entry(String name, String content) {
        return new Entry(name, content);
    }

    private record Entry(String name, String content) {
    }
}
