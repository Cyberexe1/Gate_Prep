package com.gateprep.api.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Keeps next months' partitions of the attempts table ready, so inserts never fall into the default partition. */
@Component
public class PartitionMaintainer {

    private static final Logger log = LoggerFactory.getLogger(PartitionMaintainer.class);

    private final JdbcClient jdbc;

    public PartitionMaintainer(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 15 2 * * *")
    public void ensure() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i <= 3; i++) {
            jdbc.sql("select ensure_attempts_partition(cast(:d as date))").param("d", java.sql.Date.valueOf(today.plusMonths(i))).query().singleRow();
        }
        log.debug("attempts partitions ensured through {}", today.plusMonths(3));
    }
}
