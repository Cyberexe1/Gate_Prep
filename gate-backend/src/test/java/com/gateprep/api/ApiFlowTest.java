package com.gateprep.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiFlowTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) throws Exception {
        Path dir = Files.createTempDirectory(Path.of("build"), "pg-");
        r.add("app.dev.pg-dir", dir::toString);
        r.add("app.content-dir", () -> "content");
    }

    @Autowired TestRestTemplate http;

    @BeforeEach
    void client() {
        // the default JDK HttpURLConnection throws on a 401 answer to a POST; the java.net.http client does not
        http.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }
    @Autowired ObjectMapper json;

    static String email = "student-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    static String access;
    static String refresh;
    static String moduleId;
    static JsonNode firstQuestion;

    private HttpEntity<?> auth(Object body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        if (access != null) h.setBearerAuth(access);
        return new HttpEntity<>(body, h);
    }

    private ResponseEntity<JsonNode> call(HttpMethod m, String url, Object body) {
        return http.exchange(url, m, auth(body), JsonNode.class);
    }

    private ObjectNode obj() {
        return json.createObjectNode();
    }

    @Test @Order(1)
    void catalogIsPublicButQuestionsAreNot() {
        var cat = http.getForEntity("/api/catalog", JsonNode.class);
        assertEquals(HttpStatus.OK, cat.getStatusCode());
        assertEquals(12, cat.getBody().get("subjects").size());
        moduleId = cat.getBody().get("subjects").get(3).get("modules").get(0).get("id").asText();   // programming & DS, first module

        var mocks = http.getForEntity("/api/catalog/mocks", JsonNode.class);
        assertEquals(5, mocks.getBody().get("mocks").size());

        assertEquals(HttpStatus.UNAUTHORIZED, http.getForEntity("/api/modules/" + moduleId + "/questions", JsonNode.class).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, http.getForEntity("/api/mocks/mock_1/questions", JsonNode.class).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, http.getForEntity("/api/images/q/m1_q01.webp", byte[].class).getStatusCode());
    }

    @Test @Order(2)
    void registerLoginAndValidation() {
        var weak = call(HttpMethod.POST, "/api/auth/register", obj().put("email", email).put("password", "short").put("name", "Asha"));
        assertEquals(HttpStatus.BAD_REQUEST, weak.getStatusCode());
        var badMail = call(HttpMethod.POST, "/api/auth/register", obj().put("email", "nope").put("password", "longenough1").put("name", "Asha"));
        assertEquals(HttpStatus.BAD_REQUEST, badMail.getStatusCode());

        var reg = call(HttpMethod.POST, "/api/auth/register", obj().put("email", email).put("password", "correct horse 1").put("name", "Asha"));
        assertEquals(HttpStatus.CREATED, reg.getStatusCode());
        assertEquals(email, reg.getBody().get("user").get("email").asText());
        assertFalse(reg.getBody().toString().contains("password"));

        var dup = call(HttpMethod.POST, "/api/auth/register", obj().put("email", email.toUpperCase()).put("password", "correct horse 1").put("name", "Asha"));
        assertEquals(HttpStatus.CONFLICT, dup.getStatusCode());

        var wrong = call(HttpMethod.POST, "/api/auth/login", obj().put("email", email).put("password", "wrong password"));
        assertEquals(HttpStatus.UNAUTHORIZED, wrong.getStatusCode());
        var ghost = call(HttpMethod.POST, "/api/auth/login", obj().put("email", "ghost@example.com").put("password", "whatever123"));
        assertEquals(HttpStatus.UNAUTHORIZED, ghost.getStatusCode());
        assertEquals(wrong.getBody().get("message").asText(), ghost.getBody().get("message").asText());

        var login = call(HttpMethod.POST, "/api/auth/login", obj().put("email", email).put("password", "correct horse 1"));
        assertEquals(HttpStatus.OK, login.getStatusCode());
        access = login.getBody().get("accessToken").asText();
        refresh = login.getBody().get("refreshToken").asText();
        assertEquals(email, call(HttpMethod.GET, "/api/me", null).getBody().get("email").asText());
    }

    @Test @Order(3)
    void practiceQuestionsAndImages() {
        var qs = call(HttpMethod.GET, "/api/modules/" + moduleId + "/questions", null);
        assertEquals(HttpStatus.OK, qs.getStatusCode());
        assertTrue(qs.getBody().size() > 10);
        firstQuestion = qs.getBody().get(0);
        for (JsonNode q : qs.getBody()) {
            if ("MCQ".equals(q.path("type").asText()) && q.path("answer").asText().length() == 1) { firstQuestion = q; break; }
        }
        String id = firstQuestion.get("id").asText();
        var img = http.exchange("/api/images/q/" + id + ".webp", HttpMethod.GET, auth(null), byte[].class);
        assertEquals(HttpStatus.OK, img.getStatusCode());
        assertEquals("image/webp", img.getHeaders().getContentType().toString());
        assertTrue(img.getBody().length > 500);
        assertEquals(HttpStatus.NOT_FOUND, http.exchange("/api/images/x/" + id + ".webp", HttpMethod.GET, auth(null), byte[].class).getStatusCode());
        assertTrue(http.exchange("/api/images/q/..%2F..%2Fapplication.webp", HttpMethod.GET, auth(null), byte[].class).getStatusCode().is4xxClientError(), "path traversal is refused");
        assertEquals(HttpStatus.NOT_FOUND, call(HttpMethod.GET, "/api/modules/nope/questions", null).getStatusCode());
    }

    @Test @Order(4)
    void attemptsAreMarkedByTheServer() {
        String id = firstQuestion.get("id").asText();
        String right = firstQuestion.get("answer").asText();
        String wrong = right.equals("A") ? "B" : "A";
        var bad = call(HttpMethod.POST, "/api/progress/attempts", obj().put("questionId", id).put("selection", wrong));
        assertEquals(HttpStatus.OK, bad.getStatusCode());
        assertFalse(bad.getBody().get("ok").asBoolean());
        var good = call(HttpMethod.POST, "/api/progress/attempts", obj().put("questionId", id).put("selection", right));
        assertTrue(good.getBody().get("ok").asBoolean());

        JsonNode p = call(HttpMethod.GET, "/api/progress", null).getBody();
        assertTrue(p.get("attempts").get(id).get("ok").asBoolean(), "latest attempt wins");
        assertEquals(moduleId, p.get("attempts").get(id).get("m").asText());

        assertEquals(HttpStatus.NO_CONTENT, call(HttpMethod.PUT, "/api/progress/viewed/" + id + "?value=true", null).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, call(HttpMethod.PUT, "/api/progress/state",
            obj().put("lastSubject", "pds").put("lastModule", moduleId).set("positions", obj().put(moduleId, 4))).getStatusCode());
        p = call(HttpMethod.GET, "/api/progress", null).getBody();
        assertEquals(moduleId, p.get("viewed").get(id).asText());
        assertEquals(4, p.get("positions").get(moduleId).asInt());
        assertEquals(moduleId, p.get("lastModule").asText());

        assertEquals(HttpStatus.NO_CONTENT, call(HttpMethod.DELETE, "/api/progress/attempts/" + id, null).getStatusCode());
        assertFalse(call(HttpMethod.GET, "/api/progress", null).getBody().get("attempts").has(id));
        assertEquals(HttpStatus.BAD_REQUEST, call(HttpMethod.POST, "/api/progress/attempts", obj().put("questionId", "m1_q01").put("selection", "A")).getStatusCode());
    }

    @Test @Order(5)
    void mockTestIsTimedAndMarkedOnTheServer() {
        var qs = call(HttpMethod.GET, "/api/mocks/mock_1/questions", null);
        assertEquals(65, qs.getBody().size());
        for (JsonNode q : qs.getBody()) {
            assertEquals("", q.path("answer").asText(""), "answers must not be sent before submitting");
            assertFalse(q.has("lo"));
        }
        assertEquals(HttpStatus.NO_CONTENT, call(HttpMethod.GET, "/api/mocks/mock_1/session", null).getStatusCode());

        var s = call(HttpMethod.POST, "/api/mocks/mock_1/start", null);
        assertEquals(HttpStatus.OK, s.getStatusCode());
        assertEquals(180 * 60_000L, s.getBody().get("durationMs").asLong());
        assertFalse(s.getBody().get("finished").asBoolean());
        assertFalse(s.getBody().has("keys"), "no answer key while the test runs");
        long startTs = s.getBody().get("startTs").asLong();
        assertEquals(startTs, call(HttpMethod.POST, "/api/mocks/mock_1/start", null).getBody().get("startTs").asLong(), "starting twice resumes");

        ObjectNode answers = obj();
        qs.getBody().forEach(q -> answers.put(q.get("id").asText(), "NAT".equals(q.get("type").asText()) ? "1" : "A"));
        ObjectNode save = obj();
        save.set("answers", answers);
        save.putArray("review").add("m1_q03");
        save.putArray("visited").add("m1_q01").add("m1_q02");
        save.put("position", 7);
        assertEquals(HttpStatus.NO_CONTENT, call(HttpMethod.PUT, "/api/mocks/mock_1/session", save).getStatusCode());
        JsonNode cur = call(HttpMethod.GET, "/api/mocks/mock_1/session", null).getBody();
        assertEquals(7, cur.get("position").asInt());
        assertEquals(65, cur.get("answers").size());

        var done = call(HttpMethod.POST, "/api/mocks/mock_1/submit", obj().set("answers", answers));
        assertEquals(HttpStatus.OK, done.getStatusCode());
        JsonNode r = done.getBody();
        assertTrue(r.get("finished").asBoolean());
        assertEquals(65, r.get("correct").asInt() + r.get("wrong").asInt() + r.get("skipped").asInt());
        assertEquals(65, r.get("keys").size(), "answer key is revealed after submitting");
        double sum = r.get("gaScore").asDouble() + r.get("csScore").asDouble();
        assertEquals(sum, r.get("score").asDouble(), 0.011);
        assertEquals(r.get("score").asDouble(), call(HttpMethod.POST, "/api/mocks/mock_1/submit", null).getBody().get("score").asDouble(), "submitting again changes nothing");
        assertEquals(HttpStatus.CONFLICT, call(HttpMethod.PUT, "/api/mocks/mock_1/session", save).getStatusCode());

        JsonNode lb = call(HttpMethod.GET, "/api/mocks/mock_1/leaderboard", null).getBody();
        assertEquals(1, lb.get("players").asInt());
        assertEquals(1, lb.get("myRank").asInt());
        assertTrue(lb.get("top").get(0).get("me").asBoolean());

        var again = call(HttpMethod.POST, "/api/mocks/mock_1/start?restart=true", null);
        assertFalse(again.getBody().get("finished").asBoolean());
    }

    @Test @Order(6)
    void refreshRotatesAndDetectsReuse() {
        var first = call(HttpMethod.POST, "/api/auth/refresh", obj().put("refreshToken", refresh));
        assertEquals(HttpStatus.OK, first.getStatusCode());
        String newRefresh = first.getBody().get("refreshToken").asText();
        assertFalse(newRefresh.equals(refresh));

        // the old token was already used: treated as stolen, so the new one is revoked too
        assertEquals(HttpStatus.UNAUTHORIZED, call(HttpMethod.POST, "/api/auth/refresh", obj().put("refreshToken", refresh)).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, call(HttpMethod.POST, "/api/auth/refresh", obj().put("refreshToken", newRefresh)).getStatusCode());

        var login = call(HttpMethod.POST, "/api/auth/login", obj().put("email", email).put("password", "correct horse 1"));
        String rt = login.getBody().get("refreshToken").asText();
        assertEquals(HttpStatus.NO_CONTENT, call(HttpMethod.POST, "/api/auth/logout", obj().put("refreshToken", rt)).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, call(HttpMethod.POST, "/api/auth/refresh", obj().put("refreshToken", rt)).getStatusCode());
    }
}
