package com.acme.supportpilot.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * The model used for the small jobs around retrieval: rewriting and expanding queries
 * (Checkpoint B) and re-ranking (Checkpoint E). These calls run on every request, before the
 * answer is generated, so they should be fast and cheap - and deterministic (temperature 0).
 *
 * <p>{@code supportpilot.rag.helper-model} can name a smaller model of the active provider,
 * e.g. gpt-4.1-nano. Empty means "the same model as the chat".
 */
@Component
public class RagHelperModel {

    private final ChatClient.Builder builder;

    public RagHelperModel(ChatClient.Builder builder, @Value("${supportpilot.rag.helper-model:}") String model) {
        var options = ChatOptions.builder().temperature(0.0);
        if (StringUtils.hasText(model)) {
            options.model(model);
        }
        this.builder = builder.defaultOptions(options);
    }

    /** A builder for helper ChatClients. Spring AI's transformers call build() on it once each. */
    public ChatClient.Builder builder() {
        return this.builder;
    }

}
