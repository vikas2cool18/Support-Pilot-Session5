package com.acme.supportpilot.knowledge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One test question for the knowledge base: which document should be found,
 * and a keyword the answer should contain. Read from data/rag-questions.csv.
 */
public record RagQuestion(String id, String question, List<String> expectedSources, String expectedKeyword) {

    public static List<RagQuestion> readCsv(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<RagQuestion> questions = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty()) {
                continue;
            }
            String[] f = line.split(",", -1);
            if (f.length != 4) {
                throw new IllegalArgumentException(file + " line " + (i + 1) + ": expected 4 fields");
            }
            questions.add(new RagQuestion(f[0].strip(), f[1].strip(), Arrays.asList(f[2].strip().split("\\|")),
                    f[3].strip()));
        }
        return questions;
    }

}
