package com.acme.supportpilot.knowledge;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

/**
 * An offline EmbeddingModel for tests. It is NOT semantic: it hashes words into 256 buckets
 * ("bag of words"), so texts that share words get similar vectors. Good enough to test
 * ingestion, search and RAG plumbing without a network.
 */
public class FakeEmbeddingModel implements EmbeddingModel {

    static final int DIMENSIONS = 256;

    private int calls;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        this.calls++;
        List<Embedding> embeddings = new ArrayList<>();
        List<String> texts = request.getInstructions();
        for (int i = 0; i < texts.size(); i++) {
            embeddings.add(new Embedding(vector(texts.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return vector(document.getText() == null ? "" : document.getText());
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    public int calls() {
        return this.calls;
    }

    static float[] vector(String text) {
        float[] v = new float[DIMENSIONS];
        for (String word : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (word.length() > 2) {
                v[Math.floorMod(word.hashCode(), DIMENSIONS)] += 1f;
            }
        }
        if (VectorMath.norm(v) == 0) {
            v[0] = 1f;                  // never return a zero vector (cosine would be undefined)
        }
        double norm = VectorMath.norm(v);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) {
                v[i] = (float) (v[i] / norm);
            }
        }
        return v;
    }

}
