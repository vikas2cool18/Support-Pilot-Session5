package com.acme.supportpilot.rag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.acme.supportpilot.knowledge.IngestionService;
import com.acme.supportpilot.knowledge.RagQuestion;
import com.acme.supportpilot.knowledge.SearchHit;

/**
 * Session 4: one technique at a time. Runs the same questions through a ladder of pipeline
 * configurations - naive, +rewrite, +expansion, +hybrid, +re-rank - and reports retrieval quality
 * (hit@N, MRR) and latency for each. Retrieval only, so it is cheap; add
 * {@code --supportpilot.rag-compare.answers=true} to also generate full answers for the last step.
 *
 * <pre>
 * mvn spring-boot:run -Dspring-boot.run.profiles=local,rag-compare
 * </pre>
 * Questions whose expected source is "none" are unanswerable; for those we report how many chunks
 * still reached the prompt (ideally 0 once re-ranking is on).
 */
@Component
@Profile("rag-compare")
class RagCompareRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RagCompareRunner.class);

    private final GroundedAssistant assistant;

    private final IngestionService ingestion;

    private final RagSettings settings;

    private final List<Path> questionFiles;

    private final Path resultsDir;

    private final boolean ingest;

    private final boolean answers;

    RagCompareRunner(GroundedAssistant assistant, IngestionService ingestion, RagSettings settings,
                     @Value("${supportpilot.rag-compare.questions:data/rag-questions.csv,data/hard-questions.csv}") List<String> files,
                     @Value("${supportpilot.homework.results-dir:results}") String resultsDir,
                     @Value("${supportpilot.rag-compare.ingest:false}") boolean ingest,
                     @Value("${supportpilot.rag-compare.answers:false}") boolean answers) {
        this.assistant = assistant;
        this.ingestion = ingestion;
        this.settings = settings;
        this.questionFiles = files.stream().map(String::strip).map(Path::of).toList();
        this.resultsDir = Path.of(resultsDir);
        this.ingest = ingest;
        this.answers = answers;
    }

    /** The ladder: each step adds one technique to the previous one. */
    Map<String, RagOptions> ladder() {
        int n = this.settings.defaults().topN();
        Map<String, RagOptions> steps = new LinkedHashMap<>();
        steps.put("1 naive", RagOptions.naive(n));
        steps.put("2 +rewrite", new RagOptions(true, 0, false, false, n));
        steps.put("3 +expand", new RagOptions(true, 2, false, false, n));
        steps.put("4 +hybrid", new RagOptions(true, 2, true, false, n));
        steps.put("5 +rerank", new RagOptions(true, 2, true, true, n));
        return steps;
    }

    @Override
    public void run(String... args) throws IOException {
        if (this.ingest) {
            this.ingestion.ingestAll();
        }
        List<RagQuestion> questions = new ArrayList<>();
        for (Path file : this.questionFiles) {
            questions.addAll(RagQuestion.readCsv(file));
        }
        List<String> rows = new ArrayList<>();
        rows.add("config,id,first_relevant_rank,chunks,latency_ms,top_source");
        List<String> summary = new ArrayList<>();

        for (Map.Entry<String, RagOptions> step : ladder().entrySet()) {
            int answerable = 0;
            int hits = 0;
            double rrSum = 0;
            long latency = 0;
            int leakedChunks = 0;
            for (RagQuestion q : questions) {
                RetrievalResult r = this.assistant.retrieve(q.question(), step.getValue(), KnowledgeFilter.NONE);
                List<SearchHit> chunks = r.chunks();
                latency += r.latencyMs();
                int rank = rankOfFirstRelevant(chunks, q.expectedSources());
                if (q.expectedSources().contains("none")) {
                    leakedChunks += chunks.size();
                }
                else {
                    answerable++;
                    hits += (rank > 0) ? 1 : 0;
                    rrSum += (rank > 0) ? 1.0 / rank : 0;
                }
                rows.add(String.join(",", step.getKey(), q.id(), String.valueOf(rank), String.valueOf(chunks.size()),
                        String.valueOf(r.latencyMs()), chunks.isEmpty() ? "-" : chunks.get(0).source()));
            }
            summary.add(String.format(Locale.ROOT, "  %-11s hit@%d %2d/%-2d   MRR %.2f   avg %5d ms   chunks for unanswerable: %d",
                    step.getKey(), this.settings.defaults().topN(), hits, answerable,
                    (answerable == 0) ? 0 : rrSum / answerable, latency / Math.max(1, questions.size()), leakedChunks));
        }

        List<String> answerLines = new ArrayList<>();
        if (this.answers) {
            RagOptions full = ladder().get("5 +rerank");
            for (RagQuestion q : questions) {
                GroundedAnswer a = this.assistant.answer(q.question(), full, KnowledgeFilter.NONE);
                long verified = a.citations().stream().filter(CitedSource::verified).count();
                answerLines.add(String.format("  %s answerable=%-5s citations %d (%d verified)  %s", q.id(), a.answerable(),
                        a.citations().size(), verified, a.answer().replaceAll("\\s+", " ")));
            }
        }

        Files.createDirectories(this.resultsDir);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path out = this.resultsDir.resolve("rag-compare-" + stamp + ".csv");
        Files.write(out, rows, StandardCharsets.UTF_8);
        log.info("""

                ===== RAG pipeline comparison - {} questions =====
                {}
                {}
                Details written to {}
                """, questions.size(), String.join("\n", summary), String.join("\n", answerLines), out.toAbsolutePath());
    }

    static int rankOfFirstRelevant(List<SearchHit> chunks, List<String> expected) {
        for (int i = 0; i < chunks.size(); i++) {
            if (expected.contains(chunks.get(i).source())) {
                return i + 1;
            }
        }
        return 0;
    }

}
