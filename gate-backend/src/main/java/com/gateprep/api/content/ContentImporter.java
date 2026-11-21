package com.gateprep.api.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateprep.api.config.AppProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.sql.Types;

/**
 * Loads the question bank from <content-dir>/data (the JSON files produced by the extraction scripts)
 * into PostgreSQL the first time the application starts on an empty database.
 */
@Component
@Order(1)
public class ContentImporter implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ContentImporter.class);

    private final JdbcClient jdbc;
    private final ObjectMapper json;
    private final AppProps props;
    private final TransactionTemplate tx;

    public ContentImporter(JdbcClient jdbc, ObjectMapper json, AppProps props, TransactionTemplate tx) {
        this.jdbc = jdbc;
        this.json = json;
        this.props = props;
        this.tx = tx;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Integer existing = jdbc.sql("select count(*) from subjects").query(Integer.class).single();
        if (existing != null && existing > 0) {
            log.info("Content already imported ({} subjects)", existing);
            return;
        }
        File data = new File(props.contentDir(), "data");
        if (!data.isDirectory()) {
            log.warn("No content folder at {} - skipping import", data.getAbsolutePath());
            return;
        }
        tx.executeWithoutResult(status -> {
            try {
                importAll(data);
            } catch (Exception e) {
                throw new IllegalStateException("Content import failed", e);
            }
        });
    }

    private void importAll(File data) throws Exception {
        int questions = 0;
        JsonNode subjects = json.readTree(new File(data, "index.json")).path("subjects");
        int si = 0;
        for (JsonNode s : subjects) {
            jdbc.sql("insert into subjects(id,name,short_name,emoji,c1,c2,position,question_count) values (:id,:name,:short,:emoji,:c1,:c2,:pos,:cnt)")
                .param("id", s.path("id").asText()).param("name", s.path("name").asText()).param("short", s.path("short").asText())
                .param("emoji", s.path("emoji").asText("")).param("c1", s.path("c1").asText()).param("c2", s.path("c2").asText())
                .param("pos", si++).param("cnt", s.path("count").asInt()).update();
            int mi = 0;
            for (JsonNode m : s.path("modules")) {
                String mid = m.path("id").asText();
                jdbc.sql("insert into modules(id,subject_id,name,position,question_count) values (:id,:sid,:name,:pos,:cnt)")
                    .param("id", mid).param("sid", s.path("id").asText()).param("name", m.path("name").asText())
                    .param("pos", mi++).param("cnt", m.path("count").asInt()).update();
                File qf = new File(data, "q/" + mid + ".json");
                if (qf.isFile()) questions += insertQuestions(json.readTree(qf), mid, null);
            }
        }
        File mockIndex = new File(data, "mock/index.json");
        int mocks = 0;
        if (mockIndex.isFile()) {
            for (JsonNode m : json.readTree(mockIndex).path("mocks")) {
                String id = m.path("id").asText();
                jdbc.sql("insert into mock_tests(id,n,title,question_count,marks,minutes,c1,c2,emoji,sections,nat_count) "
                        + "values (:id,:n,:title,:qc,:marks,:min,:c1,:c2,:emoji,cast(:sections as jsonb),:nat)")
                    .param("id", id).param("n", m.path("n").asInt()).param("title", m.path("title").asText())
                    .param("qc", m.path("questions").asInt()).param("marks", m.path("marks").asInt()).param("min", m.path("minutes").asInt())
                    .param("c1", m.path("c1").asText()).param("c2", m.path("c2").asText()).param("emoji", m.path("emoji").asText(""))
                    .param("sections", m.path("sections").toString()).param("nat", m.path("nat").asInt()).update();
                File qf = new File(data, "mock/" + id + ".json");
                if (qf.isFile()) questions += insertQuestions(json.readTree(qf), null, id);
                mocks++;
            }
        }
        log.info("Imported {} subjects, {} mock tests, {} questions", subjects.size(), mocks, questions);
    }

    private int insertQuestions(JsonNode list, String moduleId, String mockId) {
        int pos = 0;
        for (JsonNode q : list) {
            var spec = jdbc.sql("insert into questions(id,module_id,mock_id,position,year,label,qno,marks,type,answer,lo,hi,has_sol,mode,src,exam,stem,options) "
                    + "values (:id,:module,:mock,:pos,:year,:label,:qno,:marks,:type,:answer,:lo,:hi,:sol,:mode,:src,:exam,cast(:stem as jsonb),cast(:options as jsonb)) "
                    + "on conflict (id) do nothing")
                .param("id", q.path("id").asText())
                .param("module", moduleId, Types.VARCHAR).param("mock", mockId, Types.VARCHAR)
                .param("pos", pos++).param("year", q.path("year").asInt()).param("label", q.path("label").asText(""))
                .param("qno", q.path("qno").asText("")).param("marks", q.path("marks").asInt())
                .param("type", q.path("type").asText("")).param("answer", q.path("answer").asText(""))
                .param("lo", q.hasNonNull("lo") ? q.get("lo").asDouble() : null, Types.DOUBLE)
                .param("hi", q.hasNonNull("hi") ? q.get("hi").asDouble() : null, Types.DOUBLE)
                .param("sol", q.path("hasSol").asBoolean()).param("mode", q.path("mode").asText("image"))
                .param("src", q.path("src").asText("")).param("exam", q.path("exam").asText(""))
                .param("stem", q.hasNonNull("stem") ? q.get("stem").toString() : null, Types.VARCHAR)
                .param("options", q.hasNonNull("options") ? q.get("options").toString() : null, Types.VARCHAR);
            spec.update();
        }
        return pos;
    }
}
