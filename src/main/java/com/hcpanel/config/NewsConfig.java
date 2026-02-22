package com.hcpanel.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Configuration for news entries displayed in the panel.
 * Loaded from news.json bundled in the JAR resources.
 */
public class NewsConfig {

    private static final Gson GSON = new GsonBuilder().create();
    private static final String RESOURCE_PATH = "/news.json";

    private List<NewsEntry> entries = new ArrayList<>();

    /**
     * Load news configuration from bundled resources.
     */
    public static NewsConfig load(HytaleLogger logger) {
        try (InputStream is = NewsConfig.class.getResourceAsStream(RESOURCE_PATH)) {
            if (is == null) {
                logger.at(Level.WARNING).log("[NewsConfig] No news.json found in resources, using empty config");
                return new NewsConfig();
            }

            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                NewsConfig config = GSON.fromJson(reader, NewsConfig.class);

                if (config == null) {
                    logger.at(Level.WARNING).log("[NewsConfig] Failed to parse news.json, using empty config");
                    return new NewsConfig();
                }

                if (config.entries == null) {
                    config.entries = new ArrayList<>();
                }

                logger.at(Level.INFO).log("[NewsConfig] Loaded " + config.entries.size() + " news entries");
                return config;
            }

        } catch (Exception e) {
            logger.at(Level.WARNING).log("[NewsConfig] Error loading news.json: " + e.getMessage());
            return new NewsConfig();
        }
    }

    public List<NewsEntry> getEntries() {
        return entries;
    }

    public NewsConfig() {}

    /**
     * Constructor accepting pre-built entries (from database).
     */
    public NewsConfig(List<NewsEntry> entries) {
        this.entries = entries != null ? entries : new ArrayList<>();
    }

    /**
     * A single news entry.
     */
    public static class NewsEntry {
        private String title;
        private String date;
        private String type;
        private String label;
        private String content;

        public NewsEntry() {}

        /**
         * Factory for database-loaded entries with explicit label.
         */
        public static NewsEntry of(String title, String date, String type, String label, String content) {
            NewsEntry e = new NewsEntry();
            e.title = title;
            e.date = date;
            e.type = type;
            e.label = label;
            e.content = content;
            return e;
        }

        public String getTitle() {
            return title != null ? title : "Untitled";
        }

        public String getDate() {
            return date != null ? date : "";
        }

        public String getType() {
            return type != null ? type : "update";
        }

        public String getContent() {
            return content != null ? content : "";
        }

        /**
         * Get display color for the type badge.
         */
        public String getTypeColor() {
            return switch (getType().toLowerCase()) {
                case "announcement" -> "#ffd700";
                case "update" -> "#4a9eff";
                case "event" -> "#9b59b6";
                case "maintenance" -> "#e67e22";
                default -> "#4a9eff";
            };
        }

        /**
         * Get display label for the type badge.
         */
        public String getTypeLabel() {
            if (label != null && !label.isEmpty()) return label;
            return getType().toUpperCase();
        }
    }
}
