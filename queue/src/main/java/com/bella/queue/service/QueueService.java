package com.bella.queue.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.time.LocalDateTime;

import com.bella.queue.model.Token;
import com.bella.queue.model.TokenStatus;

@Service
public class QueueService {

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @Value("${queue.max.counters}")
        private int maxCounters;

        private int counter = 1;

        // Generate Token
        public Token generateToken() {

                String tokenNumber = "T" + counter++;
                LocalDateTime now = LocalDateTime.now();

                jdbcTemplate.update(
                                "INSERT INTO TOKEN (TOKEN_NUMBER, STATUS, CREATED_TIME) VALUES (?, ?, ?)",
                                tokenNumber, TokenStatus.WAITING.name(), now);

                return new Token(tokenNumber, TokenStatus.WAITING);
        }

        // Serve Next
        public Token serveNext() {

                List<Map<String, Object>> waiting = jdbcTemplate.queryForList(
                                "SELECT * FROM TOKEN WHERE STATUS='WAITING' ORDER BY ID LIMIT 1");

                if (waiting.isEmpty()) {
                        throw new RuntimeException("No waiting tokens.");
                }

                Map<String, Object> row = waiting.get(0);
                String tokenNumber = (String) row.get("TOKEN_NUMBER");

                jdbcTemplate.update(
                                "UPDATE TOKEN SET STATUS='SERVING', SERVED_TIME=? WHERE TOKEN_NUMBER=?",
                                LocalDateTime.now(), tokenNumber);

                return new Token(tokenNumber, TokenStatus.SERVING);
        }

        // Complete
        public void completeToken(String tokenNumber) {

                jdbcTemplate.update(
                                "UPDATE TOKEN SET STATUS='COMPLETED' WHERE TOKEN_NUMBER=?",
                                tokenNumber);

                try {
                        serveNext();
                } catch (RuntimeException e) {
                        // No waiting tokens left — that's fine, nothing to serve next
                }
        }

        // Reset Queue
        public void resetQueue() {
                jdbcTemplate.update("DELETE FROM TOKEN");
                counter = 1;
        }

        // Summary
        public Map<String, Long> getSummary() {

                Map<String, Long> summary = new HashMap<>();

                summary.put("total",
                                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM TOKEN", Long.class));

                summary.put("waiting",
                                jdbcTemplate.queryForObject(
                                                "SELECT COUNT(*) FROM TOKEN WHERE STATUS='WAITING'", Long.class));

                summary.put("serving",
                                jdbcTemplate.queryForObject(
                                                "SELECT COUNT(*) FROM TOKEN WHERE STATUS='SERVING'", Long.class));

                summary.put("completed",
                                jdbcTemplate.queryForObject(
                                                "SELECT COUNT(*) FROM TOKEN WHERE STATUS='COMPLETED'", Long.class));

                return summary;
        }

        public List<Token> getAllTokens() {

                return jdbcTemplate.query(
                                "SELECT * FROM TOKEN ORDER BY ID",
                                (rs, rowNum) -> new Token(
                                                rs.getString("TOKEN_NUMBER"),
                                                TokenStatus.valueOf(rs.getString("STATUS")),
                                                rs.getObject("CREATED_TIME", java.time.LocalDateTime.class),
                                                rs.getObject("SERVED_TIME", java.time.LocalDateTime.class)));
        }

        public List<Token> getCurrentlyServing() {

                return jdbcTemplate.query(
                                "SELECT * FROM TOKEN WHERE STATUS='SERVING'",
                                (rs, rowNum) -> new Token(
                                                rs.getString("TOKEN_NUMBER"),
                                                TokenStatus.valueOf(rs.getString("STATUS"))));
        }

        public Double getAverageWaitingTimeSeconds() {

                Double avg = jdbcTemplate.queryForObject(
                                """
                                                SELECT AVG(DATEDIFF('SECOND', CREATED_TIME, SERVED_TIME))
                                                FROM TOKEN
                                                WHERE SERVED_TIME IS NOT NULL
                                                """,
                                Double.class);

                return avg == null ? 0.0 : avg;
        }
}
