package com.bella.queue.controller;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.bella.queue.model.Token;
import com.bella.queue.service.QueueService;

@RestController
@RequestMapping("/queue")
public class HelloController {

    @Autowired
    private QueueService queueService;

    // 🔐 Login Check Method
    private void checkLogin(HttpSession session) {
        if (session.getAttribute("user") == null) {
            throw new RuntimeException("Unauthorized - Please login first");
        }
    }

    // Generate token
    @GetMapping("/generate")
    public Token generateToken(HttpSession session) {
        checkLogin(session);
        return queueService.generateToken();
    }

    // Serve next token
    @GetMapping("/serve")
    public Token serveNext(HttpSession session) {
        checkLogin(session);
        return queueService.serveNext();
    }

    // Complete token
    @GetMapping("/complete/{tokenNumber}")
    public String completeToken(@PathVariable String tokenNumber,
                                HttpSession session) {
        checkLogin(session);
        queueService.completeToken(tokenNumber);
        return "Token " + tokenNumber + " completed successfully.";
    }

    // Summary
    @GetMapping("/summary")
    public Map<String, Long> getSummary(HttpSession session) {
        checkLogin(session);
        return queueService.getSummary();
    }

    // Get all tokens
    @GetMapping("/all")
    public List<Token> getAllTokens(HttpSession session) {
        checkLogin(session);
        return queueService.getAllTokens();
    }

    // Currently serving tokens
    @GetMapping("/serving")
    public List<Token> getCurrentlyServing(HttpSession session) {
        checkLogin(session);
        return queueService.getCurrentlyServing();
    }

    // Average waiting time
    @GetMapping("/average-waiting-time")
    public Double getAverageWaitingTime(HttpSession session) {
        checkLogin(session);
        return queueService.getAverageWaitingTimeSeconds();
    }

    @GetMapping("/reset")
    public String resetQueue() {
        queueService.resetQueue();
        return "Queue reset successfully.";
    }
}
