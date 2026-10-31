package com.acme.supportpilot.rag;

import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * Checkpoint C: restrict retrieval to part of the knowledge base, using the catalog metadata
 * that Session 3 attached to every chunk (doc_type, product_area).
 *
 * <p>The filter is built with {@link FilterExpressionBuilder}, never by gluing user input into a
 * filter string - the same reason we never build SQL by string concatenation. Values are also
 * restricted to a safe pattern.
 *
 * <p>In Session 9 the most important filter - the customer's tenant - will come from the
 * authenticated user, not from a request parameter.
 *
 * @param docType e.g. policy, faq, guide, reference, pricing, release-notes (or null)
 * @param area    e.g. billing, platform, security, api (or null)
 */
public record KnowledgeFilter(String docType, String area) {

    /** Context key under which the filter travels through the pipeline (read by the hybrid retriever). */
    public static final String KEY = "supportpilot_knowledge_filter";

    public static final KnowledgeFilter NONE = new KnowledgeFilter(null, null);

    private static final Pattern VALUE = Pattern.compile("[a-z][a-z-]{0,29}");

    public KnowledgeFilter {
        docType = clean("docType", docType);
        area = clean("area", area);
    }

    private static String clean(String name, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!VALUE.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must be lower-case letters and dashes, e.g. 'policy'");
        }
        return value;
    }

    public boolean isEmpty() {
        return this.docType == null && this.area == null;
    }

    /** The portable Spring AI filter, or null when there is nothing to filter on. */
    public Filter.Expression toExpression() {
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        if (this.docType != null && this.area != null) {
            return b.and(b.eq("doc_type", this.docType), b.eq("product_area", this.area)).build();
        }
        if (this.docType != null) {
            return b.eq("doc_type", this.docType).build();
        }
        if (this.area != null) {
            return b.eq("product_area", this.area).build();
        }
        return null;
    }

    /** Puts the filter into a pipeline context, where VectorStoreDocumentRetriever looks for it. */
    public void putInto(Map<String, Object> context) {
        context.put(KEY, this);
        if (!isEmpty()) {
            context.put(VectorStoreDocumentRetriever.FILTER_EXPRESSION, toExpression());
        }
    }

    public static KnowledgeFilter from(Map<String, Object> context) {
        return (context.get(KEY) instanceof KnowledgeFilter filter) ? filter : NONE;
    }

}
