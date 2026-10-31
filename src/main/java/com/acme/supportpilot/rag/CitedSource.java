package com.acme.supportpilot.rag;

/**
 * One citation in an answer, resolved to the document it points to.
 *
 * @param number   the source number used in the answer, e.g. 2 for "[2]"
 * @param source   file name of the cited chunk
 * @param page     PDF page, if any
 * @param section  section or document title
 * @param quote    supporting text quoted by the model (Checkpoint E), or null
 * @param verified true if the number exists and the quote really appears in that chunk
 */
public record CitedSource(int number, String source, Object page, String section, String quote, boolean verified) {
}
