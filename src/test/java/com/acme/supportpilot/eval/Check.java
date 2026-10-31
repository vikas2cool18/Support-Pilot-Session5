package com.acme.supportpilot.eval;

/** The result of one deterministic check on one answer, e.g. ("citations-verified", false, "1 unverified"). */
public record Check(String name, boolean passed, String detail) {
}
