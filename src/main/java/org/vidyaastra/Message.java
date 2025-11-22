package org.vidyaastra;

public class Message {
    public final String role;
    public final String content;

    public Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    @Override
    public String toString() {
        return String.format("[%s]: %s", role.toUpperCase(), content);
    }
}
