package com.acme.supportpilot.eval;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

/**
 * Checkpoint C: LLM-as-a-judge. A second model grades each answer against a written rubric
 * (src/test/resources/eval/judge-v1.st) on three 1-5 scales. Temperature 0, and ideally a
 * different, stronger model than the one being judged - models tend to prefer their own style.
 */
public class LlmJudge {

    static final String RUBRIC = "eval/judge-v1.st";

    private final ChatClient chat;

    private final String template;

    public LlmJudge(ChatClient.Builder builder, String model, String template) {
        var options = ChatOptions.builder().temperature(0.0);
        if (StringUtils.hasText(model)) {
            options.model(model);
        }
        this.chat = builder.defaultOptions(options).build();
        this.template = template;
    }

    public static LlmJudge fromClasspath(ChatClient.Builder builder, String model) throws IOException {
        return new LlmJudge(builder, model, new ClassPathResource(RUBRIC).getContentAsString(StandardCharsets.UTF_8));
    }

    /**
     * @param context the numbered sources exactly as the answering model saw them
     */
    public JudgeScore judge(String question, String expectedAnswer, String answer, String context) {
        JudgeScore score = this.chat.prompt()
                .user(u -> u.text(this.template)
                        .param("question", question)
                        .param("expected", expectedAnswer)
                        .param("context", context.isBlank() ? "(no sources were retrieved)" : context)
                        .param("answer", answer))
                .call()
                .entity(JudgeScore.class);
        if (score == null) {
            throw new IllegalStateException("The judge returned no score");
        }
        return score;
    }

}
