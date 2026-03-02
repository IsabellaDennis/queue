package com.bella.queue.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;

import com.bella.queue.model.Token;
import com.bella.queue.model.TokenStatus;

@Service
public class QueueService {

    private Queue<Token> queue = new LinkedList<>();
    private List<Token> allTokens = new ArrayList<>();
    private int counter = 1;

    @Value("${queue.max.counters}")
    private int maxCounters;

    @Autowired
    private FileStorageService fileStorageService;

    // Load saved tokens when application starts
    @PostConstruct
    public void init() {

        List<Token> savedTokens = fileStorageService.loadTokens();
        allTokens.addAll(savedTokens);

        for (Token token : savedTokens) {

            if (token.getStatus() == TokenStatus.WAITING) {
                queue.add(token);
            }

            int number = Integer.parseInt(token.getTokenNumber().substring(1));
            if (number >= counter) {
                counter = number + 1;
            }
        }
    }

    // Generate new token
    public Token generateToken() {

        Token token = new Token("T" + counter++, TokenStatus.WAITING);
        queue.add(token);
        allTokens.add(token);

        fileStorageService.saveTokens(allTokens);
        return token;
    }

    // Serve next token (With Counter Limit)
    public Token serveNext() {

        long servingCount = allTokens.stream()
                .filter(t -> t.getStatus() == TokenStatus.SERVING)
                .count();

        if (servingCount >= maxCounters) {
            throw new RuntimeException("All counters are busy.");
        }

        Token token = queue.poll();

        if (token != null) {
            token.setStatus(TokenStatus.SERVING);
            token.setServedTime(LocalDateTime.now());
            fileStorageService.saveTokens(allTokens);
        }

        return token;
    }

    // Complete token + Auto-Serve Next
    public Token completeToken(String tokenNumber) {

        for (Token token : allTokens) {

            if (token.getTokenNumber().equals(tokenNumber)) {

                if (token.getStatus() == TokenStatus.SERVING) {

                    token.setStatus(TokenStatus.COMPLETED);

                    // Auto-serve next waiting token
                    Token nextToken = queue.poll();
                    if (nextToken != null) {
                        nextToken.setStatus(TokenStatus.SERVING);
                        nextToken.setServedTime(LocalDateTime.now());
                    }

                    fileStorageService.saveTokens(allTokens);
                    return token;
                }

                throw new RuntimeException(
                        "Token must be SERVING before it can be COMPLETED.");
            }
        }

        throw new RuntimeException("Token not found.");
    }

    // Currently Serving Tokens
    public List<Token> getCurrentlyServing() {
        return allTokens.stream()
                .filter(t -> t.getStatus() == TokenStatus.SERVING)
                .toList();
    }

    // Average Waiting Time (in seconds)
    public double getAverageWaitingTimeSeconds() {

        List<Token> completedTokens = allTokens.stream()
                .filter(t -> t.getStatus() == TokenStatus.COMPLETED
                        && t.getServedTime() != null)
                .toList();

        if (completedTokens.isEmpty()) {
            return 0;
        }

        long totalSeconds = completedTokens.stream()
                .mapToLong(t -> Duration
                        .between(t.getCreatedTime(), t.getServedTime())
                        .getSeconds())
                .sum();

        return (double) totalSeconds / completedTokens.size();
    }

    // Summary endpoint logic
    public Map<String, Long> getSummary() {

        long waiting = allTokens.stream()
                .filter(t -> t.getStatus() == TokenStatus.WAITING)
                .count();

        long serving = allTokens.stream()
                .filter(t -> t.getStatus() == TokenStatus.SERVING)
                .count();

        long completed = allTokens.stream()
                .filter(t -> t.getStatus() == TokenStatus.COMPLETED)
                .count();

        Map<String, Long> summary = new HashMap<>();
        summary.put("total", (long) allTokens.size());
        summary.put("waiting", waiting);
        summary.put("serving", serving);
        summary.put("completed", completed);

        return summary;
    }

    // View all tokens
    public List<Token> getAllTokens() {
        return allTokens;
    }
}
