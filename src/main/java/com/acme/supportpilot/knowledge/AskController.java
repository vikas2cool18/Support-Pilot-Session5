package com.acme.supportpilot.knowledge;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.acme.supportpilot.web.InvalidRequestException;

/** Checkpoint E: GET /ask?q=How long do PRO customers have to ask for a refund? */
@RestController
public class AskController {

    private final KnowledgeAssistant assistant;

    public AskController(KnowledgeAssistant assistant) {
        this.assistant = assistant;
    }

    @GetMapping("/ask")
    public KnowledgeAnswer ask(@RequestParam String q) {
        if (q.isBlank() || q.length() > 1000) {
            throw new InvalidRequestException("The question must be 1-1000 characters.");
        }
        return this.assistant.ask(q);
    }

}
