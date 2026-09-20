package com.multiai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * SharedMemory
 * ------------
 * The "context bus": whatever one AI produces is stored here so the next
 * AI in the pipeline can use it as context. A production system would use
 * a vector database (Pinecone, Weaviate) or a knowledge graph here; this
 * demo uses a plain JSON file so you can open it and see exactly how
 * context flows between steps.
 */
public class SharedMemory {

    private final Path storePath;

    public SharedMemory() {
        this(Path.of("context_store.json"));
    }

    public SharedMemory(Path storePath) {
        this.storePath = storePath;
        if (!Files.exists(storePath)) {
            writeRaw(new JSONObject().put("entries", new JSONArray()));
        }
    }

    private JSONObject readRaw() {
        try {
            String content = Files.readString(storePath, StandardCharsets.UTF_8);
            return new JSONObject(content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read context store: " + e.getMessage(), e);
        }
    }

    private void writeRaw(JSONObject data) {
        try {
            Files.writeString(storePath, data.toString(2), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write context store: " + e.getMessage(), e);
        }
    }

    /**
     * Adds a new context entry.
     *
     * @param sourceAi which AI produced this (e.g. "gemini-coder")
     * @param role     what kind of content this is (e.g. "generated_code")
     * @param content  the actual text output
     * @param tags     optional labels, used to filter context later
     */
    public JSONObject addEntry(String sourceAi, String role, String content, List<String> tags) {
        JSONObject data = readRaw();
        JSONArray entries = data.getJSONArray("entries");

        JSONObject entry = new JSONObject();
        entry.put("id", entries.length() + 1);
        entry.put("source_ai", sourceAi);
        entry.put("role", role);
        entry.put("content", content);
        entry.put("tags", new JSONArray(tags));
        entry.put("timestamp", Instant.now().toString());

        entries.put(entry);
        data.put("entries", entries);
        writeRaw(data);
        return entry;
    }

    /**
     * Retrieves all entries relevant to the given tags. This is also
     * where you would enforce permissions in a real system — e.g. only
     * returning tags a given AI is allowed to see.
     */
    public List<JSONObject> getContextFor(List<String> tags) {
        JSONArray entries = readRaw().getJSONArray("entries");
        List<JSONObject> result = new ArrayList<>();

        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            if (tags == null || tags.isEmpty()) {
                result.add(entry);
                continue;
            }
            JSONArray entryTags = entry.getJSONArray("tags");
            for (int j = 0; j < entryTags.length(); j++) {
                if (tags.contains(entryTags.getString(j))) {
                    result.add(entry);
                    break;
                }
            }
        }
        return result;
    }

    /** Formats matching context entries as readable text to hand to the next AI. */
    public String formatAsPromptContext(List<String> tags) {
        List<JSONObject> entries = getContextFor(tags);
        if (entries.isEmpty()) {
            return "(no prior context)";
        }
        StringBuilder sb = new StringBuilder();
        for (JSONObject e : entries) {
            sb.append("[").append(e.getString("source_ai")).append(" | ")
                    .append(e.getString("role")).append("]\n")
                    .append(e.getString("content")).append("\n\n---\n");
        }
        return sb.toString();
    }

    public void reset() {
        writeRaw(new JSONObject().put("entries", new JSONArray()));
    }
}
