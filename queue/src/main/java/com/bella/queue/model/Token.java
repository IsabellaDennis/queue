package com.bella.queue.model;

public class Token {

    private String tokenNumber;
    private String status;

    public Token(String tokenNumber, String status) {
        this.tokenNumber = tokenNumber;
        this.status = status;
    }

    public String getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(String tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
