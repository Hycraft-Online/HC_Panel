package com.hcpanel.database;

import com.hcpanel.config.NewsConfig.NewsEntry;
import com.hypixel.hytale.logger.HytaleLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class NewsRepository {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("HC_Panel-News");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    private final DatabaseManager databaseManager;

    public NewsRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Load published news articles from the database.
     */
    public List<NewsEntry> loadPublishedNews(int limit) {
        String sql = """
            SELECT title, tag, snippet, content, created_at
            FROM news_articles
            WHERE published = true AND type = 'news'
            ORDER BY created_at DESC
            LIMIT ?
            """;

        List<NewsEntry> entries = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String title = rs.getString("title");
                    String tag = rs.getString("tag");
                    String snippet = rs.getString("snippet");
                    String content = rs.getString("content");
                    Timestamp createdAt = rs.getTimestamp("created_at");

                    String type = mapTagToType(tag);
                    String label = mapTagToLabel(tag);
                    String date = createdAt != null
                            ? createdAt.toLocalDateTime().format(DATE_FORMAT)
                            : "";

                    // Use snippet if available, otherwise truncate content
                    String displayContent = snippet != null && !snippet.isBlank()
                            ? truncateContent(snippet, 200)
                            : truncateContent(content, 200);

                    entries.add(NewsEntry.of(title, date, type, label, displayContent));
                }
            }

        } catch (SQLException e) {
            LOGGER.at(Level.SEVERE).log("Failed to load news from database: " + e.getMessage());
        }

        LOGGER.at(Level.INFO).log("Loaded " + entries.size() + " news articles from database");
        return entries;
    }

    private static String mapTagToType(String tag) {
        if (tag == null) return "update";
        return switch (tag) {
            case "Hotfix" -> "maintenance";
            case "Update" -> "update";
            case "Major Update" -> "announcement";
            case "Content" -> "event";
            case "Guide", "Tips" -> "update";
            default -> "update";
        };
    }

    private static String mapTagToLabel(String tag) {
        if (tag == null) return "NEWS";
        return switch (tag) {
            case "Hotfix" -> "HOTFIX";
            case "Update" -> "UPDATE";
            case "Major Update" -> "MAJOR UPDATE";
            case "Content" -> "CONTENT";
            case "Guide" -> "GUIDE";
            case "Tips" -> "TIPS";
            default -> "NEWS";
        };
    }

    private static String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        // Truncate at last space before maxLength
        int cutoff = content.lastIndexOf(' ', maxLength);
        if (cutoff <= 0) cutoff = maxLength;
        return content.substring(0, cutoff) + "...";
    }
}
