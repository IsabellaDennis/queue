package com.bella.queue.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
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

    // View all tokens
    @GetMapping("/all")
    public List<Token> getAllTokens() {
        return queueService.getAllTokens();
    }
}
