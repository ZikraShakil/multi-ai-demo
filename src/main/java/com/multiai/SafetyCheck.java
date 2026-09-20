package com.multiai;

/**
 * SafetyCheck
 * -----------
 * A minimal example of the security layer the original proposal called
 * for: content is screened before being written to shared memory, so one
 * AI cannot silently inject instructions that a later AI in the pipeline
 * would treat as trusted context.
 * <p>
 * This is intentionally simple. A real system needs proper prompt-injection
 * detection, PII scrubbing, and per-AI permission checks on what context
 * each step is allowed to read or write.
 */
public class SafetyCheck {

    private static final String[] BANNED_PATTERNS = {
            "ignore previous instructions",
            "system prompt:",
            "disregard all prior"
    };

    private SafetyCheck() {
    }

    public static boolean isSafe(String content) {
        String lowered = content.toLowerCase();
        for (String pattern : BANNED_PATTERNS) {
            if (lowered.contains(pattern)) {
                return false;
            }
        }
        return true;
    }
}
