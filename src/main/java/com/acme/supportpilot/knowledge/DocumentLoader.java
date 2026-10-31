package com.acme.supportpilot.knowledge;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Checkpoint C (Extract): turn files into Spring AI Documents.
 * Each reader keeps useful structure as metadata:
 *   PDF      -> one Document per page, with page_number
 *   Markdown -> one Document per section, with the heading as "title"
 *   DOCX, HTML, TXT (Tika) -> one Document with all the text
 */
@Component
public class DocumentLoader {

    public List<Document> load(Path file) {
        Resource resource = new FileSystemResource(file);
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf")) {
            return new PagePdfDocumentReader(resource).get();
        }
        if (name.endsWith(".md")) {
            MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                    .withHorizontalRuleCreateDocument(true)
                    .withIncludeCodeBlock(true)
                    .withIncludeBlockquote(true)
                    .build();
            return new MarkdownDocumentReader(resource, config).get();
        }
        return new TikaDocumentReader(resource).get();
    }

}
