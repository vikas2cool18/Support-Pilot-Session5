package com.acme.supportpilot.rag;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import com.acme.supportpilot.chat.FakeChatModel;
import com.acme.supportpilot.knowledge.SearchHit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** The Session 4 pipeline end to end, offline. */
class GroundedAssistantTest {

    private final SimpleVectorStore store = RagFixtures.store();

    private RagPipelineFactory pipeline(FakeChatModel helper) {
        return RagFixtures.factory(this.store, "v2", helper, query -> List.<Document>of());
    }

    private static GroundedAnswer ask(GroundedAssistant assistant, String question, RagOptions options) {
        return assistant.answer(question, options, KnowledgeFilter.NONE);
    }

    /** The model's reply: plain text until Checkpoint D, a structured answer from Checkpoint E. */
    private static String reply(String answer) {
        return "{\"answer\": \"" + answer + "\", \"citations\": [], \"answerable\": true}";
    }


    @Test
    void rewriteAndExpansionUseTheHelperModelAndAreTraced() {
        FakeChatModel helper = new FakeChatModel("PRO plan refund period",
                "refund deadline annual subscription\nmoney back PRO plan");
        FakeChatModel model = new FakeChatModel(reply("30 days [1]."));
        GroundedAssistant assistant = RagFixtures.assistant(model, pipeline(helper));

        GroundedAnswer answer = ask(assistant, "hey, can i still get my money back??", new RagOptions(true, 2, false, false, 1));

        assertThat(helper.callCount()).isEqualTo(2);           // one rewrite, one expansion
        assertThat(model.callCount()).isEqualTo(1);            // one answer
        assertThat(answer.trace()).filteredOn(s -> s.name().equals("rewrite"))
                .singleElement().satisfies(s -> assertThat(s.detail()).isEqualTo("PRO plan refund period"));
        assertThat(answer.trace()).filteredOn(s -> s.name().equals("expand"))
                .singleElement().satisfies(s -> assertThat(s.detail()).contains("money back PRO plan"));
        // The answer prompt still shows the customer's original question, not the rewrite.
        assertThat(model.lastPrompt().getUserMessage().getText()).contains("hey, can i still get my money back??");
    }

    @Test
    void metadataFilterRestrictsRetrieval() {
        FakeChatModel model = new FakeChatModel(reply("Dark mode is in version 3.3 [1]."));
        GroundedAssistant assistant = RagFixtures.assistant(model, pipeline(new FakeChatModel("unused")));

        GroundedAnswer answer = assistant.answer("How long do PRO customers have to ask for a refund?",
                RagOptions.naive(1), new KnowledgeFilter("release-notes", null));

        assertThat(answer.sources()).extracting(SearchHit::source).containsExactly("release-notes.md");
    }


    @Test
    void structuredAnswerWithAnExactQuoteIsVerified() {
        FakeChatModel model = new FakeChatModel("""
                {"answer": "PRO customers have 30 days [1].",
                 "citations": [{"source": 1, "quote": "PRO customers can get a full refund within 30 days of the annual payment."}],
                 "answerable": true}""");
        GroundedAssistant assistant = RagFixtures.assistant(model, pipeline(new FakeChatModel("unused")));

        GroundedAnswer answer = ask(assistant, "How long do PRO customers have to ask for a refund?", RagOptions.naive(1));

        assertThat(answer.answerable()).isTrue();
        assertThat(model.lastPrompt().getUserMessage().getText()).contains("[1] refund-policy.pdf, page 1");
        assertThat(answer.citations()).singleElement().satisfies(c -> {
            assertThat(c.source()).isEqualTo("refund-policy.pdf");
            assertThat(c.verified()).isTrue();
        });
    }

    @Test
    void paraphrasedQuoteIsNotVerified() {
        FakeChatModel model = new FakeChatModel("""
                {"answer": "You have a month [1].",
                 "citations": [{"source": 1, "quote": "Customers on PRO have one month to request their money back."}],
                 "answerable": true}""");
        GroundedAssistant assistant = RagFixtures.assistant(model, pipeline(new FakeChatModel("unused")));

        GroundedAnswer answer = ask(assistant, "How long do PRO customers have to ask for a refund?", RagOptions.naive(1));

        assertThat(answer.citations()).singleElement().satisfies(c -> assertThat(c.verified()).isFalse());
    }

    @Test
    void whenTheRerankerKeepsNothingTheModelSeesNoSourcesAndTheAnswerIsNotAnswerable() {
        FakeChatModel helper = new FakeChatModel("{\"scores\": [{\"id\": 1, \"score\": 1}, {\"id\": 2, \"score\": 0}]}");
        FakeChatModel model = new FakeChatModel("""
                {"answer": "I don't know. Please contact Acme Cloud support.", "citations": [], "answerable": false}""");
        GroundedAssistant assistant = RagFixtures.assistant(model, pipeline(helper));

        GroundedAnswer answer = ask(assistant, "How much does the on-premise edition cost?", new RagOptions(false, 0, false, true, 5));

        assertThat(answer.answerable()).isFalse();
        assertThat(answer.sources()).isEmpty();
        assertThat(answer.citations()).isEmpty();
        assertThat(model.lastPrompt().getUserMessage().getText()).contains("No information was found");
    }

}
