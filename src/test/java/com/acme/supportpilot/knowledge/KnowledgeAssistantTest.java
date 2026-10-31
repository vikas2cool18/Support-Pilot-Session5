package com.acme.supportpilot.knowledge;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.ByteArrayResource;

import com.acme.supportpilot.chat.FakeChatModel;

import static org.assertj.core.api.Assertions.assertThat;

/** Checkpoint E, offline: retrieved chunks must reach the prompt, and sources must come back. */
class KnowledgeAssistantTest {

    @Test
    void retrievedChunksAreAddedToThePromptAndReturnedAsSources() {
        SimpleVectorStore store = SimpleVectorStore.builder(new FakeEmbeddingModel()).build();
        store.add(List.of(
                new Document("PRO customers can get a full refund within 30 days of the first payment.",
                        Map.of("source", "refund-policy.pdf")),
                new Document("Dark mode is available in the mobile app from version 3.3.",
                        Map.of("source", "release-notes.md"))));
        FakeChatModel model = new FakeChatModel("PRO customers have 30 days to request a refund.");
        KnowledgeAssistant assistant = new KnowledgeAssistant(ChatClient.builder(model), store,
                new ByteArrayResource("You are SupportPilot.".getBytes(StandardCharsets.UTF_8)), 1, 0.0);

        KnowledgeAnswer answer = assistant.ask("How long do PRO customers have to ask for a refund?");

        assertThat(answer.answer()).contains("30 days");
        assertThat(answer.sources()).extracting(SearchHit::source).containsExactly("refund-policy.pdf");
        String userMessage = model.lastPrompt().getUserMessage().getText();
        assertThat(userMessage).contains("full refund within 30 days").doesNotContain("Dark mode");
    }

}
