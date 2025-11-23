package org.vidyaastra;

public class ChatCompletionResponse {

    private ChatCompletionResponse() {
        // utility
    }

    /**
     * Extracts the assistant's first message content from an OpenAI
     * chat.completion JSON response of the form:
     * { "choices": [ { "message": { "role": "assistant", "content": "..." } } ] }
     *
     * This is a very small, non-robust extractor based on string operations
     * to avoid any external JSON library, but is sufficient for the known
     * OpenAI response shape.
     */
    public static String extractFirstMessageContent(String json) {
        if (json == null || json.isEmpty()) {
            return "Error: Empty response.";
        }

        // 1) Find the "choices" array
        String choicesKey = "\"choices\"";
        int choicesIdx = json.indexOf(choicesKey);
        if (choicesIdx < 0) {
            return "Error: No 'choices' field found in response.";
        }

        // Narrow to substring starting at choices
        String fromChoices = json.substring(choicesIdx);

        // 2) Find the first opening '[' after "choices"
        int arrayStart = fromChoices.indexOf('[');
        if (arrayStart < 0) {
            return "Error: Malformed 'choices' array in response.";
        }
        String fromFirstBracket = fromChoices.substring(arrayStart + 1); // after '['

        // 3) Find the first "message" object within the first choice
        String messageKey = "\"message\"";
        int messageIdx = fromFirstBracket.indexOf(messageKey);
        if (messageIdx < 0) {
            return "Error: No 'message' field found in first choice.";
        }

        String fromMessage = fromFirstBracket.substring(messageIdx);

        // 4) Inside the message object, find the "content" field
        // We need to be tolerant of whitespace around the colon
        String contentKey = "\"content\"";
        int contentKeyIdx = fromMessage.indexOf(contentKey);
        if (contentKeyIdx < 0) {
            return "Error: No 'content' field found in message.";
        }

        // Now find the colon after "content", skipping any whitespace
        int colonIdx = contentKeyIdx + contentKey.length();
        while (colonIdx < fromMessage.length() && Character.isWhitespace(fromMessage.charAt(colonIdx))) {
            colonIdx++;
        }
        if (colonIdx >= fromMessage.length() || fromMessage.charAt(colonIdx) != ':') {
            return "Error: Malformed 'content' field in message.";
        }

        // Skip the colon and any whitespace after it
        colonIdx++;
        while (colonIdx < fromMessage.length() && Character.isWhitespace(fromMessage.charAt(colonIdx))) {
            colonIdx++;
        }

        // Now we should be at the opening quote of the content string
        if (colonIdx >= fromMessage.length() || fromMessage.charAt(colonIdx) != '"') {
            return "Error: 'content' value is not a string.";
        }

        // Start reading the string after the opening quote
        int start = colonIdx + 1;
        return readJsonString(fromMessage, start);
    }

    // Reads a JSON string value starting at the first character after the opening quote
    private static String readJsonString(String json, int start) {
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                if (c == 'n') {
                    sb.append('\n');
                } else if (c == 'r') {
                    sb.append('\r');
                } else if (c == 't') {
                    sb.append('\t');
                } else if (c == 'u') {
                    // Handle Unicode escape sequences like \u003c
                    if (i + 4 < json.length()) {
                        try {
                            String hex = json.substring(i + 1, i + 5);
                            int codePoint = Integer.parseInt(hex, 16);
                            sb.append((char) codePoint);
                            i += 4; // Skip the next 4 hex digits
                        } catch (NumberFormatException e) {
                            sb.append(c); // If parsing fails, just append the character
                        }
                    } else {
                        sb.append(c);
                    }
                } else {
                    sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break; // end of string
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
