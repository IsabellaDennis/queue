package com.bella.queue.service;

import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;

import com.bella.queue.model.Token;

@Service
public class QueueService {

    private Queue<Token> queue = new LinkedList<>();
    private List<Token> allTokens = new ArrayList<>();
    private int counter = 1;

    @Autowired
    private FileStorageService fileStorageService;

    // Load tokens when application starts
    @PostConstruct
    public void init() {
        List<Token> savedTokens = fileStorageService.loadTokens();
        allTokens.addAll(savedTokens);

        for (Token token : savedTokens) {
            if (token.getStatus().equals("WAITING")) {
                queue.add(token);
            }

            // Update counter to continue numbering
            int number = Integer.parseInt(token.getTokenNumber().substring(1));
            if (number >= counter) {
                counter = number + 1;
            }
        }
    }

    // Generate Token
    public Token generateToken() {
        Token token = new Token("T" + counter++, "WAITING");
        queue.add(token);
        allTokens.add(token);

        fileStorageService.saveTokens(allTokens);
        return token;
    }

    // Serve Next (FIFO)
    public Token serveNext() {
        Token token = queue.poll();

        if (token != null) {
            token.setStatus("SERVING");
            fileStorageService.saveTokens(allTokens);
        }

        return token;
    }

    // View All Tokens
    public List<Token> getAllTokens() {
        return allTokens;
    }
}
