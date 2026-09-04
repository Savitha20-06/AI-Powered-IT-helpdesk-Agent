package com.helpdesk.itagent.rag;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class TextExtractorTest {

    private final TextExtractor extractor = new TextExtractor();

    @Test
    void extractsUtf8TextAndMarkdownFiles(@TempDir Path tempDir) throws Exception {
        Path textFile = Files.writeString(tempDir.resolve("guide.txt"), "TXT content", StandardCharsets.UTF_8);
        Path markdownFile = Files.writeString(tempDir.resolve("guide.md"), "# Markdown", StandardCharsets.UTF_8);

        assertThat(extractor.extract(textFile, "GUIDE.TXT")).isEqualTo("TXT content");
        assertThat(extractor.extract(markdownFile, "guide.md")).isEqualTo("# Markdown");
    }

    @Test
    void rejectsUnsupportedFileTypes(@TempDir Path tempDir) throws Exception {
        Path file = Files.writeString(tempDir.resolve("guide.csv"), "a,b", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> extractor.extract(file, "guide.csv"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void convertsReadFailuresToBadRequest(@TempDir Path tempDir) {
        Path missingFile = tempDir.resolve("missing.txt");

        assertThatThrownBy(() -> extractor.extract(missingFile, "missing.txt"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
