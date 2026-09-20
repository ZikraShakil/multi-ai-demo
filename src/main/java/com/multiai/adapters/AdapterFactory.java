package com.multiai.adapters;

/**
 * AdapterFactory
 * --------------
 * Creates an AIAdapter by provider name. This is the single switch point
 * for upgrading from the free tier (Gemini) to a paid provider (OpenAI or
 * Claude) later: change the "provider" string passed in Main.java, and
 * everything else in the pipeline keeps working unchanged.
 * <p>
 * Supported provider values: "gemini", "openai", "claude"
 */
public class AdapterFactory {

    private AdapterFactory() {
    }

    public static AIAdapter create(String provider, String model, String name, String systemPrompt) {
        switch (provider.toLowerCase()) {
            case "gemini":
                return new GeminiAdapter(model, name, systemPrompt);
            case "openai":
                return new OpenAIAdapter(model, name, systemPrompt);
            case "claude":
                return new ClaudeAdapter(model, name, systemPrompt);
            default:
                throw new IllegalArgumentException(
                        "Unknown provider: " + provider + ". Supported: gemini, openai, claude");
        }
    }
}
