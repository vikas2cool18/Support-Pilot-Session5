package com.acme.supportpilot.eval;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.ai.chat.client.ChatClient;

import com.acme.supportpilot.chat.FakeChatModel;

import static org.assertj.core.api.Assertions.assertThat;

class LlmJudgeTest {

    @Test
    void sendsQuestionContextAndAnswerAndParsesTheScores() throws IOException {
        FakeChatModel model = new FakeChatModel("""
                {"reasoning": "Supported by [1].", "faithfulness": 5, "relevance": 4, "correctness": 5}""");
        LlmJudge judge = LlmJudge.fromClasspath(ChatClient.builder(model), "");

        JudgeScore score = judge.judge("How long for a refund?", "30 days", "30 days [1].", "[1] refund-policy.pdf\\nWithin 30 days.");

        assertThat(score.faithfulness()).isEqualTo(5);
        assertThat(score.relevance()).isEqualTo(4);
        String prompt = model.lastPrompt().getUserMessage().getText();
        assertThat(prompt).contains("FAITHFULNESS").contains("How long for a refund?").contains("refund-policy.pdf");
    }

    @Test
    void scoresOutsideTheScaleAreClamped() {
        assertThat(new JudgeScore("x", 9, 0, 3)).isEqualTo(new JudgeScore("x", 5, 1, 3));
    }

}
