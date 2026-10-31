package com.acme.supportpilot.rag;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

import com.acme.supportpilot.chat.FakeChatModel;

import static com.acme.supportpilot.rag.ReciprocalRankFusionTest.doc;
import static org.assertj.core.api.Assertions.assertThat;

class LlmRerankerTest {

    private static LlmReranker reranker(FakeChatModel model) {
        return new LlmReranker(new RagHelperModel(ChatClient.builder(model), ""), RagFixtures.settings());
    }

    private final List<Document> candidates = List.of(doc("a"), doc("b"), doc("c"));

    @Test
    void reordersByScoreDropsWeakCandidatesAndKeepsTopN() {
        FakeChatModel model = new FakeChatModel("""
                {"scores": [{"id": 1, "score": 4}, {"id": 2, "score": 9}, {"id": 3, "score": 1}]}""");

        List<Document> result = reranker(model).rerank(new Query("question"), this.candidates, 5);

        assertThat(result).extracting(Document::getId).containsExactly("b", "a");   // c is below min-relevance 3
        assertThat(result.get(0).getMetadata()).containsEntry("rerank_score", 9);
        assertThat(model.lastPrompt().getUserMessage().getText()).contains("id 2: text b");
    }

    @Test
    void fallsBackToRetrievalOrderWhenTheModelAnswerIsUnusable() {
        FakeChatModel model = new FakeChatModel("Sorry, I can't rate these.");

        List<Document> result = reranker(model).rerank(new Query("question"), this.candidates, 2);

        assertThat(result).extracting(Document::getId).containsExactly("a", "b");
    }

}
