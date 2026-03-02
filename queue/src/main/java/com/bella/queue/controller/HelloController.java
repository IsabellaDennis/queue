package com.bella.queue.controller;

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

    // Generate token
    @GetMapping("/generate")
    public Token generateToken() {
        return queueService.generateToken();
    }

    // Serve next token (changed to GET for browser testing)
    @GetMapping("/serve")
    public Token serveNext() {
        return queueService.serveNext();
    }

    // Complete token (changed to GET for testing)
    @GetMapping("/complete/{tokenNumber}")
    public String completeToken(@PathVariable String tokenNumber) {
        queueService.completeToken(tokenNumber);
        return "Token " + tokenNumber + " completed successfully.";
    }

    // Summary
    @GetMapping("/summary")
    public Map<String, Long> getSummary() {
        return queueService.getSummary();
    }

    // Get all tokens
    @GetMapping("/all")
    public List<Token> getAllTokens() {
        return queueService.getAllTokens();
    }

    // Currently serving tokens
    @GetMapping("/serving")
    public List<Token> getCurrentlyServing() {
        return queueService.getCurrentlyServing();
    }

    // Average waiting time
    @GetMapping("/average-waiting-time")
    public Double getAverageWaitingTime() {
        return queueService.getAverageWaitingTimeSeconds();
    }
}
