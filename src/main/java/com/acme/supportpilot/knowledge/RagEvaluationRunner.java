package com.acme.supportpilot.knowledge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Homework: measure retrieval and answers for one chunking setting.
 * Re-ingests the knowledge base with the current settings, then for each question checks:
 *   - retrieval: is an expected document in the top-k results, and at which rank?
 *   - answer:    does the RAG answer contain the expected keyword?
 *
 * <pre>
 * mvn spring-boot:run -Dspring-boot.run.profiles=local,rag-eval
 * mvn spring-boot:run -Dspring-boot.run.profiles=local,rag-eval \
 *     -Dspring-boot.run.arguments="--supportpilot.knowledge.chunk-size=256"
 * </pre>
 */
@Component
@Profile("rag-eval")
class RagEvaluationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RagEvaluationRunner.class);

    private final IngestionService ingestion;

    private final VectorStore vectorStore;

    private final KnowledgeAssistant assistant;

    private final Path questionsFile;

    private final Path resultsDir;

    private final int topK;

    RagEvaluationRunner(IngestionService ingestion, VectorStore vectorStore, KnowledgeAssistant assistant,
                        @Value("${supportpilot.rag-eval.questions:data/rag-questions.csv}") String questionsFile,
                        @Value("${supportpilot.homework.results-dir:results}") String resultsDir,
                        @Value("${supportpilot.knowledge.top-k:5}") int topK) {
        this.ingestion = ingestion;
        this.vectorStore = vectorStore;
        this.assistant = assistant;
        this.questionsFile = Path.of(questionsFile);
        this.resultsDir = Path.of(resultsDir);
        this.topK = topK;
    }

    @Override
    public void run(String... args) throws IOException {
        IngestionReport report = this.ingestion.ingestAll();
        List<RagQuestion> questions = RagQuestion.readCsv(this.questionsFile);

        int hits = 0;
        int keywordHits = 0;
        double reciprocalRankSum = 0;
        long promptTokens = 0;
        long latency = 0;
        List<String> rows = new ArrayList<>();
        rows.add("id,first_relevant_rank,top_source,keyword_in_answer,prompt_tokens,latency_ms,answer");
        List<String> lines = new ArrayList<>();

        for (RagQuestion q : questions) {
            List<SearchHit> results = this.vectorStore.similaritySearch(
                    SearchRequest.builder().query(q.question()).topK(this.topK).build())
                    .stream().map(SearchHit::from).toList();
            int rank = 0;
            for (int i = 0; i < results.size(); i++) {
                if (q.expectedSources().contains(results.get(i).source())) {
                    rank = i + 1;
                    break;
                }
            }
            KnowledgeAnswer answer = this.assistant.ask(q.question());
            boolean keyword = answer.answer().toLowerCase(Locale.ROOT)
                    .contains(q.expectedKeyword().toLowerCase(Locale.ROOT));

            hits += (rank > 0) ? 1 : 0;
            reciprocalRankSum += (rank > 0) ? 1.0 / rank : 0;
            keywordHits += keyword ? 1 : 0;
            promptTokens += (answer.promptTokens() != null) ? answer.promptTokens() : 0;
            latency += answer.latencyMs();

            String top = results.isEmpty() ? "-" : results.get(0).source();
            lines.add(String.format("  %s  rank %-2s  keyword %-3s  top: %s", q.id(), rank > 0 ? rank : "-",
                    keyword ? "yes" : "NO", top));
            rows.add(String.join(",", q.id(), String.valueOf(rank), top, String.valueOf(keyword),
                    String.valueOf(answer.promptTokens()), String.valueOf(answer.latencyMs()),
                    "\"" + answer.answer().replace("\"", "\"\"").replaceAll("\\s+", " ") + "\""));
        }

        int n = questions.size();
        Files.createDirectories(this.resultsDir);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path out = this.resultsDir.resolve("rag-" + report.chunking().replace('/', '-') + "-" + stamp + ".csv");
        Files.write(out, rows, StandardCharsets.UTF_8);

        log.info("""

                ===== RAG evaluation - chunking {} - {} chunks - {} questions =====
                  hit@{}            {}/{}
                  MRR              {}
                  keyword found    {}/{}
                  avg prompt tokens {}   avg latency {} ms
                {}
                Details written to {}
                """, report.chunking(), report.totalChunks(), n, this.topK, hits, n,
                String.format(Locale.ROOT, "%.2f", (n == 0) ? 0 : reciprocalRankSum / n), keywordHits, n,
                (n == 0) ? 0 : promptTokens / n, (n == 0) ? 0 : latency / n, String.join("\n", lines),
                out.toAbsolutePath());
    }

}
