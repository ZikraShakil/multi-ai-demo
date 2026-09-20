package com.multiai.adapters;

import com.multiai.Config;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * ClaudeAdapter
 * -------------
 * Calls Anthropic's Messages API. Not free — requires a billed
 * Anthropic account. Included so you can switch to it later by changing
 * one line in Main.java (see AdapterFactory), without touching
 * SharedMemory or the orchestration logic.
 * <p>
 * Requires environment variable: ANTHROPIC_API_KEY
 * Get a key at: https://console.anthropic.com
 */
public class ClaudeAdapter implements AIAdapter {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    private final String apiKey;
    private final String model;
    private final String systemPrompt;
    private final String name;
    private final HttpClient client = HttpClient.newHttpClient();

    public ClaudeAdapter(String model, String name, String systemPrompt) {
        this.apiKey = Config.get("ANTHROPIC_API_KEY");
        if (this.apiKey == null || this.apiKey.isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY environment variable is not set.");
        }
        this.model = model;
        this.name = name;
        this.systemPrompt = systemPrompt;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String run(String task, String context) throws IOException, InterruptedException {
        String userPrompt = task;
        if (context != null && !context.equals("(no prior context)")) {
            userPrompt = "Prior context:\n" + context + "\n\nNow do this task:\n" + task;
        }

        JSONArray messages = new JSONArray()
                .put(new JSONObject().put("role", "user").put("content", userPrompt));

        JSONObject body = new JSONObject()
                .put("model", model)
                .put("max_tokens", 1500)
                .put("system", systemPrompt)
                .put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Claude API error (" + response.statusCode() + "): " + response.body());
        }

        JSONObject json = new JSONObject(response.body());
        JSONArray contentBlocks = json.getJSONArray("content");

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < contentBlocks.length(); i++) {
            JSONObject block = contentBlocks.getJSONObject(i);
            if ("text".equals(block.optString("type"))) {
                result.append(block.getString("text"));
            }
        }
        return result.toString();
    }
}
