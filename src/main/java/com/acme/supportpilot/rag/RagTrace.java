package com.acme.supportpilot.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What happened inside the retrieval pipeline for one request: each stage, how long it took
 * and what it produced. Passed to the pipeline as an advisor parameter, so every component can
 * find it in {@code Query.context()}. Thread-safe, because searches for several query variants
 * run in parallel.
 */
public final class RagTrace {

    /** Advisor-context key under which the trace travels through the pipeline. */
    public static final String KEY = "supportpilot_rag_trace";

    /** One step: e.g. "rewrite", 412 ms, "reset password link expired". */
    public record Stage(String name, long ms, String detail) {
    }

    private final List<Stage> stages = new ArrayList<>();

    private long lastEndNanos = System.nanoTime();

    public synchronized void record(String name, long startNanos, String detail) {
        long now = System.nanoTime();
        String text = (detail == null) ? "" : detail.replaceAll("\\s+", " ").strip();
        this.stages.add(new Stage(name, (now - startNanos) / 1_000_000,
                text.length() <= 300 ? text : text.substring(0, 297) + "..."));
        this.lastEndNanos = Math.max(this.lastEndNanos, now);
    }

    /** When the most recent stage ended (or when the trace was created) - the start of generation. */
    public synchronized long lastEndNanos() {
        return this.lastEndNanos;
    }

    public synchronized List<Stage> stages() {
        return List.copyOf(this.stages);
    }

    /** The trace stored in a pipeline context, or a throw-away one if there is none (e.g. in tests). */
    public static RagTrace from(Map<String, Object> context) {
        return (context.get(KEY) instanceof RagTrace trace) ? trace : new RagTrace();
    }

}
