package com.gateprep.api.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateprep.api.content.ContentDtos.GradeInfo;
import com.gateprep.api.content.ContentDtos.MockDto;
import com.gateprep.api.content.ContentService;
import com.gateprep.api.content.Grader;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Mock tests. The server keeps the clock (start time + duration) and marks the answers,
 * so neither the timer nor the score can be changed from the phone.
 */
@Service
public class MockService {

    /** Grace period for the last request after the clock reaches zero (slow network). */
    private static final long GRACE_MS = 60_000L;

    public record Key(String answer, Double lo, Double hi) {}

    /** Same field names as the app's own MockSession class. */
    public record SessionDto(String mockId, long startTs, long durationMs, Map<String, String> answers, Set<String> review,
                             Set<String> visited, int position, boolean finished, long finishTs, double score, int correct,
                             int wrong, int skipped, double gaScore, double csScore, long serverNow, Map<String, Key> keys) {}

    public record SaveRequest(Map<String, String> answers, Set<String> review, Set<String> visited, Integer position) {}

    public record SubmitRequest(Map<String, String> answers) {}

    public record LeaderRow(int rank, String name, double score, boolean me) {}

    public record Summary(SessionDto session, Double best, int attempts) {}

    public record Leaderboard(List<LeaderRow> top, Integer myRank, Double myBest, int players) {}

    private final JdbcClient jdbc;
    private final ContentService content;
    private final ObjectMapper json;

    public MockService(JdbcClient jdbc, ContentService content, ObjectMapper json) {
        this.jdbc = jdbc;
        this.content = content;
        this.json = json;
    }

    // ------------------------------------------------------------------ session lifecycle

    public Optional<SessionDto> current(UUID user, String mockId) {
        return latest(user, mockId).map(row -> toDto(finalizeIfExpired(user, mockId, row)));
    }

    @Transactional
    public SessionDto start(UUID user, String mockId, boolean restart) {
        MockDto info = content.mocks().mocks().stream().filter(m -> m.id().equals(mockId)).findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown mock test"));
        Optional<Row> existing = latest(user, mockId).map(r -> finalizeIfExpired(user, mockId, r));
        if (existing.isPresent() && !existing.get().finished && !restart) return toDto(existing.get());
        if (existing.isPresent() && !existing.get().finished) {
            // restarting while a test is still running abandons it with its answers marked
            submit(user, mockId, null);
        }
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into mock_sessions(id, user_id, mock_id, started_at, duration_ms) values (:id,:u,:m,:t,:d)")
            .param("id", id).param("u", user).param("m", mockId).param("t", Timestamp.from(Instant.now()))
            .param("d", info.minutes() * 60_000L).update();
        return toDto(latest(user, mockId).orElseThrow());
    }

    @Transactional
    public void save(UUID user, String mockId, SaveRequest r) {
        Row row = latest(user, mockId).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No running test"));
        if (row.finished) throw new ResponseStatusException(HttpStatus.CONFLICT, "Test already submitted");
        if (System.currentTimeMillis() > row.startedAt + row.durationMs + GRACE_MS) return; // too late, the next read finalises it
        jdbc.sql("update mock_sessions set answers = cast(:a as jsonb), review = cast(:r as jsonb), visited = cast(:v as jsonb), position = :p where id = :id")
            .param("a", write(r.answers() == null ? Map.of() : r.answers()))
            .param("r", write(r.review() == null ? Set.of() : r.review()))
            .param("v", write(r.visited() == null ? Set.of() : r.visited()))
            .param("p", r.position() == null ? 0 : r.position()).param("id", row.id).update();
    }

    /** Marks the test. [finalAnswers] may be null (use what was last saved). */
    @Transactional
    public SessionDto submit(UUID user, String mockId, Map<String, String> finalAnswers) {
        Row row = latest(user, mockId).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No running test"));
        if (row.finished) return toDto(row);
        long now = System.currentTimeMillis();
        boolean inTime = now <= row.startedAt + row.durationMs + GRACE_MS;
        Map<String, String> answers = (finalAnswers != null && inTime) ? finalAnswers : row.answers;
        return toDto(finish(row, answers, Math.min(now, row.startedAt + row.durationMs)));
    }

    private Row finalizeIfExpired(UUID user, String mockId, Row row) {
        if (!row.finished && System.currentTimeMillis() > row.startedAt + row.durationMs + GRACE_MS) {
            return finish(row, row.answers, row.startedAt + row.durationMs);
        }
        return row;
    }

    private Row finish(Row row, Map<String, String> answers, long finishedAtMs) {
        double score = 0, ga = 0, cs = 0;
        int ok = 0, bad = 0, skip = 0;
        for (GradeInfo g : content.mockGrading(row.mockId)) {
            String sel = answers.get(g.id());
            double m = Grader.marks(g, sel);
            score += m;
            if ("GA".equals(g.exam())) ga += m; else cs += m;
            if (sel == null || sel.isBlank()) skip++;
            else if (Grader.isCorrect(g, sel)) ok++;
            else bad++;
        }
        jdbc.sql("update mock_sessions set finished = true, finished_at = :f, answers = cast(:a as jsonb), score = :s, correct = :ok, wrong = :bad, "
                + "skipped = :skip, ga_score = :ga, cs_score = :cs where id = :id")
            .param("f", new Timestamp(finishedAtMs)).param("a", write(answers)).param("s", round(score)).param("ok", ok).param("bad", bad)
            .param("skip", skip).param("ga", round(ga)).param("cs", round(cs)).param("id", row.id).update();
        return latestById(row.id);
    }

    // ------------------------------------------------------------------ overview of all mocks (for the Mock Test tab)

    public Map<String, Summary> summaries(UUID user) {
        Map<String, Summary> out = new LinkedHashMap<>();
        for (MockDto m : content.mocks().mocks()) {
            SessionDto s = current(user, m.id()).map(d -> new SessionDto(d.mockId(), d.startTs(), d.durationMs(), d.answers(), d.review(),
                d.visited(), d.position(), d.finished(), d.finishTs(), d.score(), d.correct(), d.wrong(), d.skipped(), d.gaScore(),
                d.csScore(), d.serverNow(), null)).orElse(null);
            record Stats(Double best, int n) {}
            Stats st = jdbc.sql("select max(score) as best, count(*) as n from mock_sessions where user_id = :u and mock_id = :m and finished")
                .param("u", user).param("m", m.id())
                .query((rs, i) -> new Stats(rs.getBigDecimal("best") == null ? null : rs.getBigDecimal("best").doubleValue(), rs.getInt("n"))).single();
            out.put(m.id(), new Summary(s, st.best(), st.n()));
        }
        return out;
    }

    // ------------------------------------------------------------------ leaderboard

    public Leaderboard leaderboard(UUID user, String mockId) {
        // best finished score of each player, ties broken by who reached it first
        record Best(UUID userId, String name, double best) {}
        List<Best> all = jdbc.sql("select s.user_id, u.display_name, max(s.score) as best, min(s.finished_at) as first_at "
                + "from mock_sessions s join users u on u.id = s.user_id where s.mock_id = :m and s.finished "
                + "group by s.user_id, u.display_name order by best desc, first_at asc")
            .param("m", mockId)
            .query((rs, i) -> new Best(rs.getObject("user_id", UUID.class), rs.getString("display_name"), rs.getDouble("best"))).list();
        List<LeaderRow> top = new java.util.ArrayList<>();
        Integer myRank = null;
        Double myBest = null;
        for (int i = 0; i < all.size(); i++) {
            Best b = all.get(i);
            boolean me = b.userId().equals(user);
            if (me) { myRank = i + 1; myBest = b.best(); }
            if (i < 20) top.add(new LeaderRow(i + 1, b.name(), b.best(), me));
        }
        return new Leaderboard(top, myRank, myBest, all.size());
    }

    // ------------------------------------------------------------------ persistence helpers

    private record Row(UUID id, String mockId, long startedAt, long durationMs, Map<String, String> answers, Set<String> review,
                       Set<String> visited, int position, boolean finished, long finishedAt, double score, int correct, int wrong,
                       int skipped, double ga, double cs) {}

    private static final String SELECT = "select id, mock_id, started_at, duration_ms, answers, review, visited, position, finished, "
        + "finished_at, score, correct, wrong, skipped, ga_score, cs_score from mock_sessions";

    private Optional<Row> latest(UUID user, String mockId) {
        return jdbc.sql(SELECT + " where user_id = :u and mock_id = :m order by started_at desc limit 1")
            .param("u", user).param("m", mockId).query((rs, i) -> row(rs)).optional();
    }

    private Row latestById(UUID id) {
        return jdbc.sql(SELECT + " where id = :id").param("id", id).query((rs, i) -> row(rs)).single();
    }

    private Row row(ResultSet rs) throws SQLException {
        Timestamp fin = rs.getTimestamp("finished_at");
        return new Row(rs.getObject("id", UUID.class), rs.getString("mock_id"), rs.getTimestamp("started_at").getTime(), rs.getLong("duration_ms"),
            read(rs.getString("answers"), new TypeReference<Map<String, String>>() {}),
            read(rs.getString("review"), new TypeReference<LinkedHashSet<String>>() {}),
            read(rs.getString("visited"), new TypeReference<LinkedHashSet<String>>() {}),
            rs.getInt("position"), rs.getBoolean("finished"), fin == null ? 0L : fin.getTime(), rs.getDouble("score"),
            rs.getInt("correct"), rs.getInt("wrong"), rs.getInt("skipped"), rs.getDouble("ga_score"), rs.getDouble("cs_score"));
    }

    private SessionDto toDto(Row r) {
        Map<String, Key> keys = null;
        if (r.finished) {                       // answers are only revealed after the test is submitted
            keys = new LinkedHashMap<>();
            for (GradeInfo g : content.mockGrading(r.mockId)) keys.put(g.id(), new Key(g.answer(), g.lo(), g.hi()));
        }
        return new SessionDto(r.mockId, r.startedAt, r.durationMs, r.answers, r.review, r.visited, r.position, r.finished, r.finishedAt,
            r.score, r.correct, r.wrong, r.skipped, r.ga, r.cs, System.currentTimeMillis(), keys);
    }

    private <T> T read(String s, TypeReference<T> type) {
        try {
            return json.readValue(s, type);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String write(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static BigDecimal round(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
}
