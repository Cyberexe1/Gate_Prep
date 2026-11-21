package com.gateprep.api.progress;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateprep.api.content.ContentDtos.GradeInfo;
import com.gateprep.api.content.ContentService;
import com.gateprep.api.content.Grader;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Practice progress of one user: attempts (marked by the server), "practised" flags and where the user stopped. */
@Service
public class ProgressService {

    public record AttemptRec(String sel, boolean ok, long ts, String m) {}

    public record Progress(Map<String, AttemptRec> attempts, Map<String, String> viewed, Map<String, Integer> positions,
                           String lastSubject, String lastModule, long lastTs) {}

    public record StateUpdate(Map<String, Integer> positions, String lastSubject, String lastModule) {}

    private final JdbcClient jdbc;
    private final ContentService content;
    private final ObjectMapper json;

    public ProgressService(JdbcClient jdbc, ContentService content, ObjectMapper json) {
        this.jdbc = jdbc;
        this.content = content;
        this.json = json;
    }

    public Progress get(UUID user) {
        Map<String, AttemptRec> attempts = new LinkedHashMap<>();
        // the most recent attempt of each question
        jdbc.sql("select distinct on (question_id) question_id, selection, ok, module_id, created_at from attempts "
                + "where user_id = :u order by question_id, created_at desc")
            .param("u", user)
            .query((org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                attempts.put(rs.getString("question_id"),
                    new AttemptRec(rs.getString("selection"), rs.getBoolean("ok"), rs.getTimestamp("created_at").getTime(), rs.getString("module_id")));
            });
        Map<String, String> viewed = new LinkedHashMap<>();
        jdbc.sql("select question_id, module_id from viewed where user_id = :u").param("u", user)
            .query((org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                viewed.put(rs.getString("question_id"), rs.getString("module_id"));
            });
        return jdbc.sql("select positions, last_subject, last_module, last_ts from user_state where user_id = :u").param("u", user)
            .query((rs, i) -> new Progress(attempts, viewed, readPositions(rs.getString("positions")),
                rs.getString("last_subject"), rs.getString("last_module"), rs.getLong("last_ts")))
            .optional()
            .orElseGet(() -> new Progress(attempts, viewed, Map.of(), null, null, 0L));
    }

    /** The server marks the answer itself, so the stored result cannot be faked by the client. */
    public AttemptRec recordAttempt(UUID user, String questionId, String selection) {
        GradeInfo g = content.gradeInfo(questionId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown question"));
        if (g.moduleId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mock questions are answered through the mock test");
        if (selection == null || selection.isBlank() || selection.length() > 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing answer");
        }
        boolean ok = Grader.isCorrect(g, selection.trim());
        Instant now = Instant.now();
        jdbc.sql("insert into attempts(user_id, question_id, selection, ok, module_id, created_at) values (:u,:q,:s,:ok,:m,:t)")
            .param("u", user).param("q", questionId).param("s", selection.trim()).param("ok", ok).param("m", g.moduleId())
            .param("t", Timestamp.from(now)).update();
        return new AttemptRec(selection.trim(), ok, now.toEpochMilli(), g.moduleId());
    }

    public void clearAttempts(UUID user, String questionId) {
        jdbc.sql("delete from attempts where user_id = :u and question_id = :q").param("u", user).param("q", questionId).update();
    }

    public void setViewed(UUID user, String questionId, boolean value) {
        if (value) {
            GradeInfo g = content.gradeInfo(questionId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown question"));
            if (g.moduleId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not a practice question");
            jdbc.sql("insert into viewed(user_id, question_id, module_id) values (:u,:q,:m) on conflict do nothing")
                .param("u", user).param("q", questionId).param("m", g.moduleId()).update();
        } else {
            jdbc.sql("delete from viewed where user_id = :u and question_id = :q").param("u", user).param("q", questionId).update();
        }
    }

    public void saveState(UUID user, StateUpdate s) {
        String positions;
        try {
            positions = json.writeValueAsString(s.positions() == null ? Map.of() : s.positions());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad positions");
        }
        jdbc.sql("insert into user_state(user_id, positions, last_subject, last_module, last_ts) values (:u, cast(:p as jsonb), :s, :m, :t) "
                + "on conflict (user_id) do update set positions = excluded.positions, last_subject = excluded.last_subject, "
                + "last_module = excluded.last_module, last_ts = excluded.last_ts")
            .param("u", user).param("p", positions).param("s", s.lastSubject(), Types.VARCHAR).param("m", s.lastModule(), Types.VARCHAR)
            .param("t", System.currentTimeMillis()).update();
    }

    private Map<String, Integer> readPositions(String s) {
        try {
            return s == null ? Map.of() : json.readValue(s, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
