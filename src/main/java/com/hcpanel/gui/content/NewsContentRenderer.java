package com.hcpanel.gui.content;

import com.hcpanel.config.NewsConfig;
import com.hcpanel.config.NewsConfig.NewsEntry;
import com.hcpanel.gui.UnifiedPanelGui.SidebarButton;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders News content into the unified panel.
 * Displays news entries from the NewsConfig.
 */
public class NewsContentRenderer {

    private final PlayerRef playerRef;
    private final NewsConfig newsConfig;

    public NewsContentRenderer(PlayerRef playerRef, NewsConfig newsConfig) {
        this.playerRef = playerRef;
        this.newsConfig = newsConfig;
    }

    /**
     * Returns sidebar buttons for the News module.
     * News has no sub-navigation, so returns empty list.
     */
    public List<SidebarButton> getSidebarButtons(String currentView) {
        return new ArrayList<>();
    }

    /**
     * Renders news content.
     */
    public void renderContent(UICommandBuilder cmd, UIEventBuilder events) {
        // Hide the content spacer and show the news section
        cmd.set("#ContentSpacer.Visible", false);
        cmd.set("#NewsSection.Visible", true);

        List<NewsEntry> entries = newsConfig.getEntries();

        // Set header info
        cmd.set("#HeaderSubtitle.Text", "Welcome, " + playerRef.getUsername());
        cmd.set("#HeaderSubtitle.Style.TextColor", "#4a9eff");

        // Count announcements vs updates
        long announcements = entries.stream()
            .filter(e -> "announcement".equalsIgnoreCase(e.getType()))
            .count();
        long updates = entries.size() - announcements;

        String infoText = "";
        if (announcements > 0 && updates > 0) {
            infoText = announcements + " announcement" + (announcements > 1 ? "s" : "") +
                       ", " + updates + " update" + (updates > 1 ? "s" : "");
        } else if (announcements > 0) {
            infoText = announcements + " announcement" + (announcements > 1 ? "s" : "");
        } else if (updates > 0) {
            infoText = updates + " update" + (updates > 1 ? "s" : "");
        }
        cmd.set("#HeaderInfo.Text", infoText);

        // Render up to 5 news entries
        for (int i = 0; i < 5; i++) {
            String cardId = "#NewsCard" + i;

            if (i < entries.size()) {
                NewsEntry entry = entries.get(i);

                cmd.set(cardId + ".Visible", true);
                cmd.set("#NewsBorder" + i + ".Background", entry.getTypeColor());
                cmd.set("#NewsType" + i + ".Text", entry.getTypeLabel());
                cmd.set("#NewsType" + i + ".Style.TextColor", entry.getTypeColor());
                cmd.set("#NewsTitle" + i + ".Text", entry.getTitle());
                cmd.set("#NewsDate" + i + ".Text", entry.getDate());
                cmd.set("#NewsContent" + i + ".Text", entry.getContent());
            } else {
                cmd.set(cardId + ".Visible", false);
            }
        }

        // Footer
        cmd.set("#FooterText.Text", "Check back for the latest updates and announcements.");
    }
}
