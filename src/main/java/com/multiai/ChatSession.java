package com.multiai;

import com.multiai.adapters.AIAdapter;

import java.util.List;

/**
 * ChatSession
 * -----------
 * Wraps an interactive conversation. Every user message and every AI
 * reply is written to SharedMemory tagged "conversation". This is what
 * makes mid-conversation AI switching possible: when you swap the active
 * adapter, the new AI is handed the *entire* conversation history pulled
 * from SharedMemory — not just the last message — so it can continue as
 * if it had been part of the conversation the whole time.
 */
public class ChatSession {

    private static final List<String> CONVERSATION_TAG = List.of("conversation");

    private final SharedMemory memory;
    private AIAdapter activeAdapter;

    public ChatSession(SharedMemory memory, AIAdapter initialAdapter) {
        this.memory = memory;
        this.activeAdapter = initialAdapter;
    }

    public String getActiveAdapterName() {
        return activeAdapter.getName();
    }

    /** Swaps the AI answering future messages. Full context is preserved automatically. */
    public void switchAdapter(AIAdapter newAdapter) {
        this.activeAdapter = newAdapter;
    }

    /**
     * Sends a user message to the currently active AI, giving it the full
     * conversation history as context, and records both sides of the
     * exchange in shared memory.
     */
    public String send(String userMessage) throws Exception {
        String fullContext = memory.formatAsPromptContext(CONVERSATION_TAG);

        memory.addEntry("user", "user_message", userMessage, CONVERSATION_TAG);

        String instruction = "Continue this conversation naturally, as if you have been "
                + "part of it from the start. Respond to the latest user message below.\n\n"
                + "Latest user message:\n" + userMessage;

        String reply = activeAdapter.run(instruction, fullContext);

        if (!SafetyCheck.isSafe(reply)) {
            throw new IllegalStateException("Safety check failed on reply from " + activeAdapter.getName());
        }

        memory.addEntry(activeAdapter.getName(), "ai_reply", reply, CONVERSATION_TAG);
        return reply;
    }
}
