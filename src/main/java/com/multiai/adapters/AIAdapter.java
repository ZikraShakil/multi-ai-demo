package com.multiai.adapters;

import java.io.IOException;

/**
 * AIAdapter
 * ---------
 * The common contract that every AI provider adapter must implement.
 * This is the "standardization layer" the proposal referred to: it lets
 * the orchestrator (Main.java) treat Gemini, OpenAI, and Claude
 * interchangeably. Swapping providers later means writing one new class
 * that implements this interface — the rest of the pipeline (SharedMemory,
 * Main.java's orchestration logic) does not change.
 */
public interface AIAdapter {

    /**
     * A human-readable identifier for this adapter instance, used as the
     * "source_ai" tag when writing to shared memory (e.g. "gemini-coder").
     */
    String getName();

    /**
     * Runs one task through this AI.
     *
     * @param task    what this AI should do
     * @param context prior context pulled from shared memory (may be null
     *                or "(no prior context)" if this is the first step)
     * @return the AI's response text
     */
    String run(String task, String context) throws IOException, InterruptedException;
}
