package com.acme.supportpilot.knowledge;

import java.util.ArrayList;
import java.util.List;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;

import org.springframework.ai.transformer.splitter.TextSplitter;

/**
 * Homework / stretch: fixed-size token windows that OVERLAP.
 * With size 512 and overlap 64, chunk 2 starts 448 tokens after chunk 1, so a sentence cut at a
 * boundary still appears whole in one of the two chunks. Spring AI's TokenTextSplitter has no
 * overlap option, which makes this a good example of writing your own TextSplitter.
 */
public class SlidingWindowSplitter extends TextSplitter {

    private final Encoding encoding = Encodings.newLazyEncodingRegistry().getEncoding(EncodingType.CL100K_BASE);

    private final int chunkSize;

    private final int overlap;

    public SlidingWindowSplitter(int chunkSize, int overlap) {
        if (chunkSize <= 0 || overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException("Need chunkSize > 0 and 0 <= overlap < chunkSize");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    @Override
    protected List<String> splitText(String text) {
        List<Integer> tokens = this.encoding.encode(text).boxed();
        List<String> chunks = new ArrayList<>();
        int step = this.chunkSize - this.overlap;
        for (int start = 0; start < tokens.size(); start += step) {
            int end = Math.min(start + this.chunkSize, tokens.size());
            IntArrayList window = new IntArrayList(end - start);
            tokens.subList(start, end).forEach(window::add);
            String chunk = this.encoding.decode(window).strip();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end == tokens.size()) {
                break;
            }
        }
        return chunks;
    }

}
