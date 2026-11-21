package com.gateprep.api.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateprep.api.content.ContentDtos.CatalogDto;
import com.gateprep.api.content.ContentDtos.GradeInfo;
import com.gateprep.api.content.ContentDtos.MockCatalogDto;
import com.gateprep.api.content.ContentDtos.MockDto;
import com.gateprep.api.content.ContentDtos.ModuleDto;
import com.gateprep.api.content.ContentDtos.QuestionDto;
import com.gateprep.api.content.ContentDtos.SubjectDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reads the (rarely changing) question content. Results are cached in memory; Redis replaces the cache later. */
@Service
public class ContentService {

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public ContentService(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Cacheable("catalog")
    public CatalogDto catalog() {
        Map<String, List<ModuleDto>> modules = new LinkedHashMap<>();
        jdbc.sql("select id, subject_id, name, question_count from modules order by subject_id, position")
            .query((org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                modules.computeIfAbsent(rs.getString("subject_id"), k -> new ArrayList<>())
                    .add(new ModuleDto(rs.getString("id"), rs.getString("name"), rs.getInt("question_count")));
            });
        List<SubjectDto> subjects = jdbc.sql("select id, name, short_name, emoji, c1, c2, question_count from subjects order by position")
            .query((rs, i) -> new SubjectDto(rs.getString("id"), rs.getString("name"), rs.getString("short_name"), rs.getString("emoji"),
                rs.getString("c1"), rs.getString("c2"), rs.getInt("question_count"), modules.getOrDefault(rs.getString("id"), List.of())))
            .list();
        return new CatalogDto(subjects);
    }

    @Cacheable("mocks")
    public MockCatalogDto mocks() {
        List<MockDto> list = jdbc.sql("select id, n, title, question_count, marks, minutes, c1, c2, emoji, sections, nat_count from mock_tests order by n")
            .query((rs, i) -> new MockDto(rs.getString("id"), rs.getInt("n"), rs.getString("title"), rs.getInt("question_count"),
                rs.getInt("marks"), rs.getInt("minutes"), rs.getString("c1"), rs.getString("c2"), rs.getString("emoji"),
                parse(rs.getString("sections")), rs.getInt("nat_count")))
            .list();
        return new MockCatalogDto(list);
    }

    @Cacheable("moduleQuestions")
    public List<QuestionDto> moduleQuestions(String moduleId) {
        List<QuestionDto> list = jdbc.sql(QUESTION_SQL + " where module_id = :m order by position")
            .param("m", moduleId).query((rs, i) -> question(rs, false)).list();
        if (list.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown module");
        return list;
    }

    /** Mock questions are sent WITHOUT answers: the server marks the test. */
    @Cacheable("mockQuestions")
    public List<QuestionDto> mockQuestions(String mockId) {
        List<QuestionDto> list = jdbc.sql(QUESTION_SQL + " where mock_id = :m order by position")
            .param("m", mockId).query((rs, i) -> question(rs, true)).list();
        if (list.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown mock test");
        return list;
    }

    @Cacheable("grading")
    public List<GradeInfo> mockGrading(String mockId) {
        return jdbc.sql(GRADE_SQL + " where mock_id = :m order by position").param("m", mockId)
            .query((rs, i) -> grade(rs)).list();
    }

    public Optional<GradeInfo> gradeInfo(String questionId) {
        return jdbc.sql(GRADE_SQL + " where id = :id").param("id", questionId).query((rs, i) -> grade(rs)).optional();
    }

    // ------------------------------------------------------------------ helpers

    private static final String QUESTION_SQL =
        "select id, year, label, qno, marks, type, answer, lo, hi, exam, src, has_sol, mode, stem, options from questions";
    private static final String GRADE_SQL =
        "select id, type, answer, lo, hi, marks, exam, module_id, mock_id from questions";

    private QuestionDto question(ResultSet rs, boolean hideAnswer) throws SQLException {
        Double lo = (Double) rs.getObject("lo");
        Double hi = (Double) rs.getObject("hi");
        return new QuestionDto(rs.getString("id"), rs.getInt("year"), rs.getString("label"), rs.getString("qno"), rs.getInt("marks"),
            rs.getString("type"), hideAnswer ? "" : rs.getString("answer"), hideAnswer ? null : lo, hideAnswer ? null : hi,
            rs.getString("exam"), rs.getString("src"), rs.getBoolean("has_sol"), rs.getString("mode"),
            parse(rs.getString("stem")), parse(rs.getString("options")));
    }

    private GradeInfo grade(ResultSet rs) throws SQLException {
        return new GradeInfo(rs.getString("id"), rs.getString("type"), rs.getString("answer"), (Double) rs.getObject("lo"),
            (Double) rs.getObject("hi"), rs.getInt("marks"), rs.getString("exam"), rs.getString("module_id"), rs.getString("mock_id"));
    }

    private JsonNode parse(String s) {
        if (s == null) return null;
        try {
            return json.readTree(s);
        } catch (Exception e) {
            throw new IllegalStateException("Bad JSON in database", e);
        }
    }
}
