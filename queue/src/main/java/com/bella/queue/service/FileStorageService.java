package com.bella.queue.service;

import org.springframework.stereotype.Service;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import com.bella.queue.model.Token;

@Service
public class FileStorageService {

    private static final String FILE_NAME = "tokens.txt";

    // Save tokens to file
    public void saveTokens(List<Token> tokens) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Token token : tokens) {
                writer.write(token.getTokenNumber() + "," + token.getStatus());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load tokens from file
    public List<Token> loadTokens() {
        List<Token> tokens = new ArrayList<>();

        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return tokens;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                tokens.add(new Token(parts[0], parts[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tokens;
    }
}