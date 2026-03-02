package com.bella.queue.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import com.bella.queue.model.Token;

@Service
public class QueueService {

    private List<Token> queue = new ArrayList<>();
    private int counter = 1;

    public Token generateToken() {
        Token token = new Token("T" + counter++, "WAITING");
        queue.add(token);
        return token;
    }

    public Token serveNext() {
        for (Token token : queue) {
            if (token.getStatus().equals("WAITING")) {
                token.setStatus("SERVING");
                return token;
            }
        }
        return null;
    }

    public List<Token> getAllTokens() {
        return queue;
    }
}
