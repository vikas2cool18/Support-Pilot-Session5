package com.acme.supportpilot.rag;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import com.acme.supportpilot.chat.FakeChatModel;
import com.acme.supportpilot.knowledge.FakeEmbeddingModel;

/** Builds the Session 4 pipeline offline: in-memory vector store, fake models, no network. */
final class RagFixtures {

    static final String REFUND = "Refund and Billing Policy. PRO customers can get a full refund within 30 days of the annual payment.";

    static final String DARK_MODE = "Release Notes. Dark mode is available in the mobile app from version 3.3.";

    private RagFixtures() {
    }

    static SimpleVectorStore store() {
        SimpleVectorStore store = SimpleVectorStore.builder(new FakeEmbeddingModel()).build();
        store.add(List.of(
                new Document(REFUND, Map.of("source", "refund-policy.pdf", "page_number", 1,
                        "doc_type", "policy", "product_area", "billing")),
                new Document(DARK_MODE, Map.of("source", "release-notes.md",
                        "doc_type", "release-notes", "product_area", "platform"))));
        return store;
    }

    static RagSettings settings() {
        // rewrite, expansions, hybrid, rerank, topN, candidates, [rrfK], [minRelevance], similarityThreshold
        return new RagSettings(false, 0, false, false, 1, 20,
                60,
                0.0);
    }

    static RagPrompts prompts(String version) {
        return new RagPrompts(version, new ClassPathResource("prompts/rag-answer-" + version + ".st"),
                new ClassPathResource("prompts/rag-empty-context.st"));
    }

    static RagPipelineFactory factory(SimpleVectorStore store, String promptVersion, FakeChatModel helper,
                                      DocumentRetriever keyword) {
        return new RagPipelineFactory(store, settings(), prompts(promptVersion),
                new RagHelperModel(ChatClient.builder(helper), ""), keyword);
    }

    static GroundedAssistant assistant(FakeChatModel model, RagPipelineFactory factory) {
        ByteArrayResource system = new ByteArrayResource("You are SupportPilot.".getBytes(StandardCharsets.UTF_8));
        return new GroundedAssistant(ChatClient.builder(model), factory, system);
    }

}
