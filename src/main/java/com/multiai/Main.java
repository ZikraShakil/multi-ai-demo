package com.multiai;

import com.multiai.adapters.AIAdapter;
import com.multiai.adapters.AdapterFactory;

import java.util.Scanner;

/**
 * Main
 * ----
 * Interactive chat with mid-conversation AI switching. This demonstrates
 * the core claim of the original proposal: an AI's context ("memory")
 * transferring cleanly to a different AI — potentially a different
 * provider entirely — so the conversation continues without you having to
 * re-paste history.
 * <p>
 * === How to use ===
 * Type messages normally to chat with the active AI.
 * <p>
 * Type /switch <provider> <model> to hand the conversation to a different
 * AI. Example:
 *   /switch gemini gemini-3.6-flash
 *   /switch openai gpt-4o
 *   /switch claude claude-sonnet-4-6
 * <p>
 * The new AI is given the FULL conversation history automatically — you
 * do not need to re-paste anything. That handoff is exactly what
 * SharedMemory + ChatSession does under the hood.
 * <p>
 * Type /exit to quit.
 * <p>
 * === Required environment variables ===
 * GEMINI_API_KEY is required for the default provider.
 * OPENAI_API_KEY / ANTHROPIC_API_KEY are only needed if you /switch to
 * those providers.
 */
public class Main {

    private static final String DEFAULT_SYSTEM_PROMPT =
            "You are a helpful assistant having an ongoing conversation with the user.";

    public static void main(String[] args) throws Exception {
        SharedMemory memory = new SharedMemory();
        memory.reset(); // fresh conversation each run; drop this line to keep history across runs

        AIAdapter initialAdapter = AdapterFactory.create(
                "gemini", "gemini-3.6-flash", "gemini-chat", DEFAULT_SYSTEM_PROMPT
        );
        ChatSession session = new ChatSession(memory, initialAdapter);

        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Multi-AI Chat ===");
        System.out.println("Active AI: " + session.getActiveAdapterName());
        System.out.println("Type a message to chat.");
        System.out.println("Type /switch <provider> <model> to hand off the conversation to a different AI.");
        System.out.println("Type /exit to quit.\n");

        while (true) {
            System.out.print("You: ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            if (input.equalsIgnoreCase("/exit")) {
                System.out.println("Conversation saved in context_store.json. Goodbye.");
                break;
            }

            if (input.startsWith("/switch")) {
                handleSwitch(input, session);
                continue;
            }

            try {
                String reply = session.send(input);
                System.out.println(session.getActiveAdapterName() + ": " + reply + "\n");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage() + "\n");
            }
        }

        scanner.close();
    }

    private static void handleSwitch(String input, ChatSession session) {
        String[] parts = input.split("\\s+");
        if (parts.length != 3) {
            System.out.println("Usage: /switch <provider> <model>");
            System.out.println("Example: /switch gemini gemini-3.6-flash\n");
            return;
        }

        String provider = parts[1];
        String model = parts[2];

        try {
            AIAdapter newAdapter = AdapterFactory.create(
                    provider, model, provider + "-chat", DEFAULT_SYSTEM_PROMPT
            );
            session.switchAdapter(newAdapter);
            System.out.println("Switched to " + session.getActiveAdapterName()
                    + ". It has been given the full conversation so far.\n");
        } catch (Exception e) {
            System.out.println("Could not switch: " + e.getMessage() + "\n");
        }
    }
}
