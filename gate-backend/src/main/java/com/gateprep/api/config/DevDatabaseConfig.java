package com.gateprep.api.config;

import com.zaxxer.hikari.HikariDataSource;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;

/** "dev" profile only: a real PostgreSQL started inside the app, so no Docker or local install is needed. */
@Configuration
@Profile("dev")
public class DevDatabaseConfig {

    @Bean(destroyMethod = "close")
    EmbeddedPostgres embeddedPostgres(@Value("${app.dev.pg-dir:./.devdb}") String dir) throws IOException {
        return EmbeddedPostgres.builder()
            .setDataDirectory(Path.of(dir, "data").toAbsolutePath())
            .setCleanDataDirectory(false)
            .start();
    }

    @Bean
    @Primary
    DataSource dataSource(EmbeddedPostgres pg) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(pg.getJdbcUrl("postgres", "postgres"));
        ds.setUsername("postgres");
        ds.setMaximumPoolSize(8);
        return ds;
    }
}
