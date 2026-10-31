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
        return RagFixtures.factory(this.store, "v1", helper);
    }

    private static GroundedAnswer ask(GroundedAssistant assistant, String question, RagOptions options) {
        return assistant.answer(question, options);
    }

    /** The model's reply: plain text until Checkpoint D, a structured answer from Checkpoint E. */
    private static String reply(String answer) {
        return answer;
    }

    @Test
    void numberedSourcesReachThePromptAndMarkersBecomeCitations() {
        FakeChatModel model = new FakeChatModel("PRO customers have 30 days to ask for a refund [1].");
        GroundedAssistant assistant = RagFixtures.assistant(model, pipeline(new FakeChatModel("unused")));

        GroundedAnswer answer = ask(assistant, "How long do PRO customers have to ask for a refund?", RagOptions.naive(1));

        String prompt = model.lastPrompt().getUserMessage().getText();
        assertThat(prompt).contains("[1] refund-policy.pdf, page 1").contains("full refund within 30 days")
                .doesNotContain("Dark mode");
        assertThat(answer.sources()).extracting(SearchHit::source).containsExactly("refund-policy.pdf");
        assertThat(answer.citations()).singleElement().satisfies(c -> {
            assertThat(c.number()).isEqualTo(1);
            assertThat(c.source()).isEqualTo("refund-policy.pdf");
            assertThat(c.verified()).isTrue();
        });
        assertThat(answer.trace()).extracting(RagTrace.Stage::name).contains("vector", "top-n", "generate");
    }

    @Test
    void citationOfASourceThatWasNotInThePromptIsNotVerified() {
        FakeChatModel model = new FakeChatModel("Refunds are possible for 30 days [1] or 60 days [4].");
        GroundedAssistant assistant = RagFixtures.assistant(model, pipeline(new FakeChatModel("unused")));

        GroundedAnswer answer = ask(assistant, "How long do PRO customers have to ask for a refund?", RagOptions.naive(1));

        assertThat(answer.citations()).extracting(CitedSource::number, CitedSource::verified)
                .containsExactly(tuple(1, true), tuple(4, false));
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


}
