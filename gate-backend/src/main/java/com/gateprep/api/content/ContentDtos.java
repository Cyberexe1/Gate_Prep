package com.gateprep.api.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/** JSON shapes sent to the Android app (field names match the app's data classes). */
public final class ContentDtos {
    private ContentDtos() {}

    public record ModuleDto(String id, String name, int count) {}

    public record SubjectDto(String id, String name, @JsonProperty("short") String shortName, String emoji,
                             String c1, String c2, int count, List<ModuleDto> modules) {}

    public record CatalogDto(List<SubjectDto> subjects) {}

    public record MockDto(String id, int n, String title, int questions, int marks, int minutes,
                          String c1, String c2, String emoji, JsonNode sections, int nat) {}

    public record MockCatalogDto(List<MockDto> mocks) {}

    public record QuestionDto(String id, int year, String label, String qno, int marks, String type, String answer,
                              Double lo, Double hi, String exam, String src, boolean hasSol, String mode,
                              JsonNode stem, JsonNode options) {}

    /** What the server needs to mark one question. */
    public record GradeInfo(String id, String type, String answer, Double lo, Double hi, int marks, String exam,
                            String moduleId, String mockId) {}
}
