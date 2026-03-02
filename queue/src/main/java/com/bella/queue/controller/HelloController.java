package com.bella.queue.controller;

import java.util.Map;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.bella.queue.model.Token;
import com.bella.queue.service.QueueService;

@RestController
@RequestMapping("/queue")
public class HelloController {

    @Autowired
    private QueueService queueService;

    // Generate new token
    @GetMapping("/generate")
    public Token generateToken() {
        return queueService.generateToken();
    }

    // Serve next token
    @GetMapping("/serve")
    public Token serveNext() {
        return queueService.serveNext();
    }

    // Complete token
    @GetMapping("/complete/{tokenNumber}")
    public Token completeToken(@PathVariable String tokenNumber) {
        return queueService.completeToken(tokenNumber);
    }

    // Get summary
    @GetMapping("/summary")
    public Map<String, Long> getSummary() {
        return queueService.getSummary();
    }

    // Get currently serving tokens
    @GetMapping("/serving")
    public List<Token> getCurrentlyServing() {
        return queueService.getCurrentlyServing();
    }

    // Get average waiting time
    @GetMapping("/average-waiting-time")
    public double getAverageWaitingTime() {
        return queueService.getAverageWaitingTimeSeconds();
    }

    // View all tokens
    @GetMapping("/all")
    public List<Token> getAllTokens() {
        return queueService.getAllTokens();
    }
}
