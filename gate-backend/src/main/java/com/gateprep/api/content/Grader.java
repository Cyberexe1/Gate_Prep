package com.gateprep.api.content;

import com.gateprep.api.content.ContentDtos.GradeInfo;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Marking rules (same as the app's offline rules): MCQ/MSQ exact set match, NAT inside [lo, hi], MCQ wrong costs 1/3 of the marks. */
public final class Grader {
    private Grader() {}

    public static boolean isCorrect(GradeInfo g, String selection) {
        if (selection == null || selection.isBlank() || g.type() == null || g.type().isBlank() || g.answer() == null || g.answer().isBlank()) {
            return false;
        }
        switch (g.type()) {
            case "MCQ", "MSQ" -> {
                return set(selection).equals(set(g.answer()));
            }
            case "NAT" -> {
                double v;
                try {
                    v = Double.parseDouble(selection.trim());
                } catch (NumberFormatException e) {
                    return false;
                }
                if (g.lo() == null) return false;
                double hi = g.hi() == null ? g.lo() : g.hi();
                return v >= g.lo() - 1e-6 && v <= hi + 1e-6;
            }
            default -> {
                return false;
            }
        }
    }

    public static double marks(GradeInfo g, String selection) {
        if (selection == null || selection.isBlank()) return 0.0;
        if (isCorrect(g, selection)) return g.marks();
        return "MCQ".equals(g.type()) ? -g.marks() / 3.0 : 0.0;
    }

    private static Set<String> set(String s) {
        return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).collect(Collectors.toSet());
    }
}
