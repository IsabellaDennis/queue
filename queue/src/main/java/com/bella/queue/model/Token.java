package com.bella.queue.model;

import java.time.LocalDateTime;

public class Token {

    private String tokenNumber;
    private TokenStatus status;
    private LocalDateTime createdTime;
    private LocalDateTime servedTime;

    // Constructor for NEW token
    public Token(String tokenNumber, TokenStatus status) {
        this.tokenNumber = tokenNumber;
        this.status = status;
        this.createdTime = LocalDateTime.now();
    }

    // Constructor for LOADED token (from file)
    public Token(String tokenNumber, TokenStatus status,
                 LocalDateTime createdTime,
                 LocalDateTime servedTime) {

        this.tokenNumber = tokenNumber;
        this.status = status;
        this.createdTime = createdTime;
        this.servedTime = servedTime;
    }

    public String getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(String tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public TokenStatus getStatus() {
        return status;
    }

    public void setStatus(TokenStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public LocalDateTime getServedTime() {
        return servedTime;
    }

    public void setServedTime(LocalDateTime servedTime) {
        this.servedTime = servedTime;
    }
}
