package org.vidyaastra;

import java.util.List;
import java.util.stream.Collectors;

public class ChatCompletionRequest {

    public final String model;
    public final List<Message> messages;
    public final double temperature;

    public ChatCompletionRequest(String model, List<Message> messages, double temperature) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
    }

    public ChatCompletionRequest(String model, List<Message> messages) {
        this(model, messages, 0.7);
    }

    // Minimal JSON serialization for the shape expected by the API
    public String toJson() {
        String messagesJson = messages.stream()
                .map(m -> "{" +
                        "\"role\":\"" + escape(m.role) + "\"," +
                        "\"content\":\"" + escape(m.content) + "\"}")
                .collect(Collectors.joining(","));

        return "{" +
                "\"model\":\"" + escape(model) + "\"," +
                "\"temperature\":" + temperature + "," +
                "\"messages\":[" + messagesJson + "]" +
                "}";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
