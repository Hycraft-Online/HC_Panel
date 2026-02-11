package com.hcpanel.gui.content;

import com.hcpanel.gui.UnifiedPanelGui.SidebarButton;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders Character module content into the unified panel.
 * Combines Attributes and Classes functionality into one unified view.
 *
 * Subpages:
 * - overview: Character stats (previously "My Stats" from Attributes)
 * - talents: Talent tree (from Classes)
 * - allocate: Stat point allocation (from Attributes)
 */
public class CharacterContentRenderer {

    private final PlayerRef playerRef;
    private final AttributesContentRenderer attributesRenderer;
    private final ClassesContentRenderer classesRenderer;

    // Unified header color for Character module
    private static final String HEADER_COLOR = "#d4af37";

    public CharacterContentRenderer(PlayerRef playerRef) {
        this.playerRef = playerRef;
        this.attributesRenderer = new AttributesContentRenderer(playerRef);
        this.classesRenderer = new ClassesContentRenderer(playerRef);
    }

    /**
     * Returns sidebar buttons for the Character module.
     */
    public List<SidebarButton> getSidebarButtons(String currentView) {
        List<SidebarButton> buttons = new ArrayList<>();

        boolean isOverview = currentView == null || "overview".equals(currentView);
        boolean isTalents = "talents".equals(currentView);
        boolean isAllocate = "allocate".equals(currentView);

        String statBadge = attributesRenderer.getStatPointsBadge();
        String talentBadge = classesRenderer.getTalentPointsBadge();

        buttons.add(new SidebarButton("Overview", "nav:character:overview", null, HEADER_COLOR, isOverview));
        buttons.add(new SidebarButton("Talents", "nav:character:talents", talentBadge, HEADER_COLOR, isTalents));
        buttons.add(new SidebarButton("Allocate", "nav:character:allocate", statBadge, HEADER_COLOR, isAllocate));

        return buttons;
    }

    /**
     * Returns combined badge for stat + talent points.
     */
    public String getCombinedPointsBadge() {
        int total = 0;

        String statBadge = attributesRenderer.getStatPointsBadge();
        if (statBadge != null) {
            total += Integer.parseInt(statBadge);
        }

        String talentBadge = classesRenderer.getTalentPointsBadge();
        if (talentBadge != null) {
            total += Integer.parseInt(talentBadge);
        }

        return total > 0 ? String.valueOf(total) : null;
    }

    /**
     * Renders content for the specified view.
     */
    public void renderContent(UICommandBuilder cmd, UIEventBuilder events, Store<EntityStore> store,
                               PlayerRef playerRef, String view) {
        // Default to overview view
        if (view == null) view = "overview";

        switch (view) {
            case "overview" -> {
                // Delegate to attributes renderer with "stats" view
                attributesRenderer.renderContent(cmd, events, store, playerRef, "stats");
            }
            case "talents" -> {
                // Delegate to classes renderer
                classesRenderer.renderContent(cmd, events, store, playerRef, "talents");
            }
            case "allocate" -> {
                // Delegate to attributes renderer with "allocate" view
                attributesRenderer.renderContent(cmd, events, store, playerRef, "allocate");
            }
            default -> {
                // Unknown view - show overview
                attributesRenderer.renderContent(cmd, events, store, playerRef, "stats");
            }
        }
    }

    /**
     * Handles stat point allocation (delegates to attributes renderer).
     */
    public boolean handleAllocation(PlayerRef playerRef, String actionData) {
        return attributesRenderer.handleAllocation(playerRef, actionData);
    }

    /**
     * Handles talent allocation (delegates to classes renderer).
     * @return error message on failure, null on success
     */
    public String handleTalentAllocation(PlayerRef playerRef, String talentId) {
        return classesRenderer.handleTalentAllocation(playerRef, talentId);
    }

    /**
     * Handles talent reset (delegates to classes renderer).
     * @return number of points refunded
     */
    public int handleTalentReset(PlayerRef playerRef) {
        return classesRenderer.handleTalentReset(playerRef);
    }
}
