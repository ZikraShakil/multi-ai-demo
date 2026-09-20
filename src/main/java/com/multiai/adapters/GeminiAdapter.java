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
 * GeminiAdapter
 * -------------
 * Calls Google's Gemini API. Gemini's free tier works without a billing
 * account (rate-limited), which is why this demo defaults to Gemini for
 * both the "coder" and "tester" roles. One class is reused for both roles
 * by giving each instance a different name and system prompt.
 * <p>
 * Requires environment variable: GEMINI_API_KEY
 * Get a free key at: https://aistudio.google.com/api-keys
 */
public class GeminiAdapter implements AIAdapter {

    private static final String API_URL_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final String apiKey;
    private final String model;
    private final String systemPrompt;
    private final String name;
    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * @param model        Gemini model id, e.g. "gemini-3.6-flash" (check
     *                     https://ai.google.dev/gemini-api/docs/models for
     *                     the current free-tier model names, as these change)
     * @param name         this adapter instance's identifier, shown as the
     *                     "source_ai" in shared memory (e.g. "gemini-coder")
     * @param systemPrompt this AI's role/instructions (e.g. "You are a Coding AI...")
     */
    public GeminiAdapter(String model, String name, String systemPrompt) {
        this.apiKey = Config.get("GEMINI_API_KEY");
        if (this.apiKey == null || this.apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY environment variable is not set.");
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

        JSONObject body = new JSONObject()
                .put("system_instruction", new JSONObject()
                        .put("parts", new JSONArray().put(new JSONObject().put("text", systemPrompt))))
                .put("contents", new JSONArray()
                        .put(new JSONObject()
                                .put("role", "user")
                                .put("parts", new JSONArray().put(new JSONObject().put("text", userPrompt)))));

        String url = String.format(API_URL_TEMPLATE, model, apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API error (" + response.statusCode() + "): " + response.body());
        }

        JSONObject json = new JSONObject(response.body());
        JSONArray candidates = json.getJSONArray("candidates");
        JSONArray parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts");

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            result.append(parts.getJSONObject(i).optString("text", ""));
        }
        return result.toString();
    }
}
