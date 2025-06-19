package com.example.carcare;

public class Message {
    public enum Sender {
        USER, BOT
    }

    private String text;
    private Sender sender;

    public Message(String text, Sender sender) {
        this.text = text;
        this.sender = sender;
    }

    public String getText() {
        return text;
    }

    public Sender getSender() {
        return sender;
    }
}
