package com.multiai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Config
 * ------
 * Reads API keys from a local "config.properties" file (in the project
 * root) if present, otherwise falls back to environment variables. This
 * lets the project run either way: with keys committed in the file
 * (convenient for a private repo you're sharing with specific people) or
 * with keys kept out of version control entirely (safer for public
 * repos).
 */
public class Config {

    private static final Properties fileProps = new Properties();
    private static boolean loaded = false;

    private Config() {
    }

    private static void loadIfNeeded() {
        if (loaded) return;
        loaded = true;

        Path configPath = Path.of("config.properties");
        if (Files.exists(configPath)) {
            try {
                fileProps.load(Files.newBufferedReader(configPath));
            } catch (IOException e) {
                System.err.println("Warning: could not read config.properties: " + e.getMessage());
            }
        }
    }

    /** Returns the value for key, checking config.properties first, then environment variables. */
    public static String get(String key) {
        loadIfNeeded();
        String value = fileProps.getProperty(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return System.getenv(key);
    }
}
