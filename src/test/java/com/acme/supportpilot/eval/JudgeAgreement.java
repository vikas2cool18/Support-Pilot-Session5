package com.acme.supportpilot.eval;

import java.util.List;

/**
 * How well the LLM judge agrees with human labels on the same answers (1-5 scales).
 *
 * @param pairs        number of (judge, human) score pairs
 * @param exact        share of pairs with exactly the same score
 * @param withinOne    share of pairs at most one point apart
 * @param meanAbsError average absolute difference
 * @param judgeBias    average of (judge - human): positive = the judge is more lenient than humans
 */
public record JudgeAgreement(int pairs, double exact, double withinOne, double meanAbsError, double judgeBias) {

    /** Each element is {judgeScore, humanScore}. */
    public static JudgeAgreement of(List<int[]> pairs) {
        if (pairs.isEmpty()) {
            return new JudgeAgreement(0, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        }
        int exact = 0;
        int withinOne = 0;
        double absSum = 0;
        double diffSum = 0;
        for (int[] p : pairs) {
            int diff = p[0] - p[1];
            exact += (diff == 0) ? 1 : 0;
            withinOne += (Math.abs(diff) <= 1) ? 1 : 0;
            absSum += Math.abs(diff);
            diffSum += diff;
        }
        int n = pairs.size();
        return new JudgeAgreement(n, (double) exact / n, (double) withinOne / n, absSum / n, diffSum / n);
    }

}
