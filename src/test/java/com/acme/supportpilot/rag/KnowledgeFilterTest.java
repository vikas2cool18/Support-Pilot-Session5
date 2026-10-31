package com.acme.supportpilot.rag;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeFilterTest {

    @Test
    void buildsTheSameExpressionAsTheTextSyntax() {
        KnowledgeFilter filter = new KnowledgeFilter("policy", "billing");

        assertThat(filter.toExpression()).isEqualTo(
                new FilterExpressionTextParser().parse("doc_type == 'policy' && product_area == 'billing'"));
        assertThat(new KnowledgeFilter("policy", null).toExpression())
                .isEqualTo(new FilterExpressionTextParser().parse("doc_type == 'policy'"));
    }

    @Test
    void emptyFilterAddsNoExpressionToTheContext() {
        Map<String, Object> context = new HashMap<>();
        new KnowledgeFilter(" ", null).putInto(context);

        assertThat(context).doesNotContainKey(VectorStoreDocumentRetriever.FILTER_EXPRESSION);
        assertThat(KnowledgeFilter.from(context).isEmpty()).isTrue();
    }

    @Test
    void rejectsValuesThatCouldInjectFilterSyntax() {
        assertThatThrownBy(() -> new KnowledgeFilter("policy' || doc_type != 'x", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
