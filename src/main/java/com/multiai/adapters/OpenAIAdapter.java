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
 * OpenAIAdapter
 * -------------
 * Calls OpenAI's Chat Completions API. Not free — requires a billed
 * OpenAI account. This class is included so you can switch to it later
 * by changing one line in Main.java (see AdapterFactory), without
 * touching SharedMemory or the orchestration logic.
 * <p>
 * Requires environment variable: OPENAI_API_KEY
 * Get a key at: https://platform.openai.com/api-keys
 */
public class OpenAIAdapter implements AIAdapter {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    private final String apiKey;
    private final String model;
    private final String systemPrompt;
    private final String name;
    private final HttpClient client = HttpClient.newHttpClient();

    public OpenAIAdapter(String model, String name, String systemPrompt) {
        this.apiKey = Config.get("OPENAI_API_KEY");
        if (this.apiKey == null || this.apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY environment variable is not set.");
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
                .put(new JSONObject().put("role", "system").put("content", systemPrompt))
                .put(new JSONObject().put("role", "user").put("content", userPrompt));

        JSONObject body = new JSONObject()
                .put("model", model)
                .put("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("OpenAI API error (" + response.statusCode() + "): " + response.body());
        }

        JSONObject json = new JSONObject(response.body());
        return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }
}
