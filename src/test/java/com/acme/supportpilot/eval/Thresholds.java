package com.acme.supportpilot.eval;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Properties;

import org.springframework.core.io.ClassPathResource;

/**
 * Checkpoint D: the quality gate's thresholds, from src/test/resources/eval/thresholds.properties.
 * Any value can be overridden for one run with -Deval.threshold.&lt;name&gt;=&lt;value&gt;.
 */
public final class Thresholds {

    private final Map<String, Double> values;

    Thresholds(Map<String, Double> values) {
        this.values = Map.copyOf(values);
    }

    public static Thresholds load() throws IOException {
        Properties p = new Properties();
        try (InputStream in = new ClassPathResource("eval/thresholds.properties").getInputStream()) {
            p.load(in);
        }
        Map<String, Double> values = new LinkedHashMap<>();
        for (String name : p.stringPropertyNames()) {
            String value = System.getProperty("eval.threshold." + name, p.getProperty(name));
            values.put(name, Double.parseDouble(value.strip()));
        }
        return new Thresholds(values);
    }

    public OptionalDouble get(String name) {
        Double v = this.values.get(name);
        return (v == null) ? OptionalDouble.empty() : OptionalDouble.of(v);
    }

}
