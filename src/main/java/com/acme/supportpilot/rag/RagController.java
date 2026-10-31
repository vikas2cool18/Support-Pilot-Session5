package com.acme.supportpilot.rag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.acme.supportpilot.web.InvalidRequestException;

/**
 * Session 4 endpoints. Every switch of the pipeline can be overridden per request, e.g.
 * <pre>
 * GET /answer?q=ERR-4471&amp;hybrid=false          compare with and without hybrid search
 * GET /retrieve?q=...&amp;rewrite=true&amp;expansions=2   only retrieval: which chunks, and why
 * GET /answer?q=...&amp;docType=policy&amp;area=billing   metadata filter (Checkpoint C)
 * </pre>
 */
@RestController
public class RagController {

    private final GroundedAssistant assistant;

    private final RagSettings settings;

    public RagController(GroundedAssistant assistant, RagSettings settings) {
        this.assistant = assistant;
        this.settings = settings;
    }

    @GetMapping("/answer")
    public GroundedAnswer answer(@RequestParam String q,
                                 @RequestParam(required = false) Boolean rewrite,
                                 @RequestParam(required = false) Integer expansions,
                                 @RequestParam(required = false) Boolean hybrid,
                                 @RequestParam(required = false) Boolean rerank) {
        return this.assistant.answer(check(q), options(rewrite, expansions, hybrid, rerank));
    }

    @GetMapping("/retrieve")
    public RetrievalResult retrieve(@RequestParam String q,
                                    @RequestParam(required = false) Boolean rewrite,
                                    @RequestParam(required = false) Integer expansions,
                                    @RequestParam(required = false) Boolean hybrid,
                                    @RequestParam(required = false) Boolean rerank) {
        return this.assistant.retrieve(check(q), options(rewrite, expansions, hybrid, rerank));
    }

    private static String check(String q) {
        if (q.isBlank() || q.length() > 1000) {
            throw new InvalidRequestException("The question must be 1-1000 characters.");
        }
        return q;
    }

    private RagOptions options(Boolean rewrite, Integer expansions, Boolean hybrid, Boolean rerank) {
        try {
            return this.settings.defaults().with(rewrite, expansions, hybrid, rerank);
        }
        catch (IllegalArgumentException ex) {
            throw new InvalidRequestException(ex.getMessage());
        }
    }

}
