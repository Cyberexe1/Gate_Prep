package com.gateprep.api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Allows a few failed logins per key (ip + email) in a time window.
 * In-memory for now; with several instances behind the load balancer this moves to Redis.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_FAILURES = 8;
    private static final long WINDOW_MS = 10 * 60 * 1000L;

    private final ConcurrentHashMap<String, Deque<Long>> failures = new ConcurrentHashMap<>();

    public void check(String key) {
        Deque<Long> q = failures.get(key);
        if (q == null) return;
        synchronized (q) {
            prune(q);
            if (q.size() >= MAX_FAILURES) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Try again in a few minutes.");
            }
        }
    }

    public void failed(String key) {
        Deque<Long> q = failures.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            prune(q);
            q.addLast(System.currentTimeMillis());
        }
    }

    public void succeeded(String key) {
        failures.remove(key);
    }

    private void prune(Deque<Long> q) {
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        while (!q.isEmpty() && q.peekFirst() < cutoff) q.pollFirst();
    }
}
