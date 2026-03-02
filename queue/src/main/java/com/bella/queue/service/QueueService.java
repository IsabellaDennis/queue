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

    // ---------------------------
    // Generate New Token
    // ---------------------------
    public Token generateToken() {

        String tokenNumber = "T" + counter++;
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(
                "INSERT INTO token (token_number, status, created_time) VALUES (?, ?, ?)",
                tokenNumber, TokenStatus.WAITING.name(), now);

        return new Token(tokenNumber, TokenStatus.WAITING);
    }

    // ---------------------------
    // Serve Next Token
    // ---------------------------
    public Token serveNext() {

        Long servingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM token WHERE status='SERVING'",
                Long.class);

        if (servingCount != null && servingCount >= maxCounters) {
            throw new RuntimeException("All counters are busy.");
        }

        List<Map<String, Object>> waiting =
                jdbcTemplate.queryForList(
                        "SELECT * FROM token WHERE status='WAITING' ORDER BY id LIMIT 1");

        if (waiting.isEmpty()) return null;

        Map<String, Object> row = waiting.get(0);
        String tokenNumber = (String) row.get("TOKEN_NUMBER");

        jdbcTemplate.update(
                "UPDATE token SET status='SERVING', served_time=? WHERE token_number=?",
                LocalDateTime.now(), tokenNumber);

        return new Token(tokenNumber, TokenStatus.SERVING);
    }

    // ---------------------------
    // Complete Token + Auto Serve Next
    // ---------------------------
    public void completeToken(String tokenNumber) {

        jdbcTemplate.update(
                "UPDATE token SET status='COMPLETED' WHERE token_number=?",
                tokenNumber);

        // 🚦 Auto serve next waiting token
        serveNext();
    }

    // ---------------------------
    // Get Summary
    // ---------------------------
    public Map<String, Long> getSummary() {

        Map<String, Long> summary = new HashMap<>();

        summary.put("total",
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM token", Long.class));

        summary.put("waiting",
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM token WHERE status='WAITING'", Long.class));

        summary.put("serving",
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM token WHERE status='SERVING'", Long.class));

        summary.put("completed",
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM token WHERE status='COMPLETED'", Long.class));

        return summary;
    }

    // ---------------------------
    // Get All Tokens
    // ---------------------------
    public List<Token> getAllTokens() {

        return jdbcTemplate.query(
                "SELECT * FROM token ORDER BY id",
                (rs, rowNum) -> {
                    Token token = new Token(
                            rs.getString("token_number"),
                            TokenStatus.valueOf(rs.getString("status")));
                    token.setServedTime(rs.getTimestamp("served_time") != null
                            ? rs.getTimestamp("served_time").toLocalDateTime()
                            : null);
                    return token;
                });
    }

    // ---------------------------
    // Get Currently Serving Tokens
    // ---------------------------
    public List<Token> getCurrentlyServing() {

        return jdbcTemplate.query(
                "SELECT * FROM token WHERE status='SERVING'",
                (rs, rowNum) ->
                        new Token(
                                rs.getString("token_number"),
                                TokenStatus.valueOf(rs.getString("status"))
                        )
        );
    }

    // ---------------------------
    // Average Waiting Time (Seconds)
    // ---------------------------
    public Double getAverageWaitingTimeSeconds() {

        Double avg = jdbcTemplate.queryForObject(
                """
                SELECT AVG(DATEDIFF('SECOND', created_time, served_time))
                FROM token
                WHERE served_time IS NOT NULL
                """,
                Double.class
        );

        return avg == null ? 0.0 : avg;
    }
}
