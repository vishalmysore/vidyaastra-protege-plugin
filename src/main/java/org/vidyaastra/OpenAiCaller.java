package org.vidyaastra;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * A lightweight, dependency-minimal class to call the OpenAI Chat Completion API
 * using Java 11+ HttpClient for networking.
 *
 * This version is flexible and can target the official OpenAI API or a proxy like the
 * LangChain4j demo endpoint by changing the base URL.
 */
public class OpenAiCaller {

    // The required path for the chat completion endpoint, appended to the base URL.
    private static final String CHAT_COMPLETION_PATH = "/chat/completions";
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String apiKey;
    private final String model;
    private final String baseUrl;

    /**
     * Initializes the caller with the API key, model, and the base URL of the service.
     * @param apiKey The OpenAI API key .
     * @param model The model name (e.g., "gpt-4o-mini" ).
     * @param baseUrl The base URL of the API (e.g., "https://api.openai.com/v1" ).
     */
    public OpenAiCaller(String apiKey, String model, String baseUrl) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("API key must be provided.");
        }
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("Base URL must be provided.");
        }
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }


    /**
     * Executes the API call and returns the complete raw JSON response.
     * @param systemPrompt The instruction to set the model's behavior.
     * @param userQuery The user's input question or task.
     * @param temperature The sampling temperature (0.0 to 2.0). Higher values make output more random.
     * @return The full JSON response from the API as a string.
     * @throws IOException If the network call or API processing fails.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If API returns a non-200 status code.
     */
    public String getFullResponse(String systemPrompt, String userQuery, double temperature) throws IOException, InterruptedException {
        String fullUrl = this.baseUrl + CHAT_COMPLETION_PATH;

        // 1. Build the list of messages
        List<Message> messages = List.of(
                new Message("system", systemPrompt),
                new Message("user", userQuery)
        );

        ChatCompletionRequest requestObject = new ChatCompletionRequest(this.model, messages, temperature);

        // Manual JSON serialization
        String jsonPayload = requestObject.toJson();

        // Build the HttpRequest
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload));

        // Only include the Authorization header if the API key is not the demo key
        if (!"demo".equalsIgnoreCase(apiKey)) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest request = builder.build();

        // Execute the request
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        String responseBody = response.body() != null ? response.body() : "";
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            System.err.println("API Call Failed. Status: " + status + ", Body: " + responseBody);
            throw new RuntimeException("API call failed: " + status + " - " + responseBody);
        }

        return responseBody;
    }

    /**
     * Executes the API call and returns the complete raw JSON response using default temperature (0.7).
     * @param systemPrompt The instruction to set the model's behavior.
     * @param userQuery The user's input question or task.
     * @return The full JSON response from the API as a string.
     * @throws IOException If the network call or API processing fails.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If API returns a non-200 status code.
     */
    public String getFullResponse(String systemPrompt, String userQuery) throws IOException, InterruptedException {
        return getFullResponse(systemPrompt, userQuery, 0.7);
    }

    /**
     * Executes the API call and returns the text response.
     * @param systemPrompt The instruction to set the model's behavior.
     * @param userQuery The user's input question or task.
     * @param temperature The sampling temperature (0.0 to 2.0). Higher values make output more random.
     * @return The text generated by the LLM.
     * @throws IOException If the network call or API processing fails.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If API returns a non-200 status code.
     */
    public String generateCompletion(String systemPrompt, String userQuery, double temperature) throws IOException, InterruptedException {
        String responseBody = getFullResponse(systemPrompt, userQuery, temperature);

        // Log full JSON for debugging the parser
        System.out.println("Raw JSON response:\n" + responseBody);

        // Very small manual parser: extract first choice.message.content
        return ChatCompletionResponse.extractFirstMessageContent(responseBody);
    }

    /**
     * Executes the API call and returns the text response using default temperature (0.7).
     * @param systemPrompt The instruction to set the model's behavior.
     * @param userQuery The user's input question or task.
     * @return The text generated by the LLM.
     * @throws IOException If the network call or API processing fails.
     * @throws InterruptedException If the request is interrupted.
     * @throws RuntimeException If API returns a non-200 status code.
     */
    public String generateCompletion(String systemPrompt, String userQuery) throws IOException, InterruptedException {
        return generateCompletion(systemPrompt, userQuery, 0.7);
    }

    /**
     * Example main method for local testing using the vishals demo endpoint.
     */
    public static void main(String[] args) {
        // We use the "demo" key for the proxy and the required model "gpt-4o-mini"
        String demoApiKey = "demo";
        String demoBaseUrl = "http://vishalmysore.github/demo/openai/v1";
        // The LangChain4j demo proxy only supports gpt-4o-mini
        String modelName = "gpt-4o-mini";

        try {
            OpenAiCaller caller = new OpenAiCaller(demoApiKey, modelName, demoBaseUrl);

            String systemPrompt = "You are a helpful assistant for Java developers, providing concise, runnable code snippets.";
            String userQuery = "Write a short Java snippet to calculate the factorial of 5 using a loop.";

            System.out.println("Sending Request to vishals Demo Proxy...");
            System.out.println("Base URL: " + demoBaseUrl);
            System.out.println("Model: " + modelName);

            String result = caller.generateCompletion(systemPrompt, userQuery);

            System.out.println("\n--- LLM Response ---");
            System.out.println(result);
            System.out.println("--------------------");

        } catch (Exception e) {
            System.err.println("An error occurred during the API call or processing.");
            e.printStackTrace();
        }
    }
}