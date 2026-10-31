package com.acme.supportpilot.eval;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.acme.supportpilot.knowledge.SearchHit;
import com.acme.supportpilot.rag.CitedSource;
import com.acme.supportpilot.rag.GroundedAnswer;

/**
 * Checkpoint B: the bottom of the evaluation pyramid. Fast, free, repeatable checks that need no
 * judgement - only the golden case and the structured answer.
 */
public final class DeterministicChecks {

    private DeterministicChecks() {
    }

    public static List<Check> run(GoldenCase c, GroundedAnswer a) {
        List<Check> checks = new ArrayList<>();
        checks.add(new Check("not-empty", a.answer() != null && !a.answer().isBlank(), ""));
        checks.add(new Check("answerable", a.answerable() == c.answerable(),
                "expected " + c.answerable() + ", got " + a.answerable()));
        if (c.answerable()) {
            Set<String> retrieved = a.sources().stream().map(SearchHit::source)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Set<String> cited = a.citations().stream().map(CitedSource::source)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            long unverified = a.citations().stream().filter(x -> !x.verified()).count();
            // Retrieval: did an expected document reach the prompt at all?
            checks.add(new Check("retrieval-hit", c.expectedSources().stream().anyMatch(retrieved::contains),
                    "retrieved " + retrieved));
            // Grounding: is the answer backed by citations whose quotes really exist?
            checks.add(new Check("has-citation", !a.citations().isEmpty(), a.citations().size() + " citation(s)"));
            checks.add(new Check("citations-verified", !a.citations().isEmpty() && unverified == 0,
                    unverified + " unverified"));
            checks.add(new Check("cites-expected-source", c.expectedSources().stream().anyMatch(cited::contains),
                    "cited " + cited));
        }
        else {
            // An honest "I don't know" must not pretend to have sources.
            checks.add(new Check("no-citations", a.citations().isEmpty(), a.citations().size() + " citation(s)"));
        }
        return checks;
    }

}
