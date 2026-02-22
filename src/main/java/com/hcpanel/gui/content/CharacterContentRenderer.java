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
        // These are optional - only created if their plugin is loaded
        AttributesContentRenderer attrTemp = null;
        try { attrTemp = new AttributesContentRenderer(playerRef); } catch (NoClassDefFoundError ignored) {}
        this.attributesRenderer = attrTemp;
        // ClassesContentRenderer has direct imports to HC_Classes - skip if not loaded
        ClassesContentRenderer classTemp = null;
        if (isClassesAvailable()) {
            try { classTemp = new ClassesContentRenderer(playerRef); } catch (NoClassDefFoundError ignored) {}
        }
        this.classesRenderer = classTemp;
    }

    /**
     * Returns sidebar buttons for the Character module.
     */
    public List<SidebarButton> getSidebarButtons(String currentView) {
        List<SidebarButton> buttons = new ArrayList<>();

        boolean isOverview = currentView == null || "overview".equals(currentView);
        boolean isTalents = "talents".equals(currentView);
        boolean isAllocate = "allocate".equals(currentView);

        String statBadge = attributesRenderer != null ? attributesRenderer.getStatPointsBadge() : null;
        String talentBadge = classesRenderer != null ? classesRenderer.getTalentPointsBadge() : null;

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

        if (attributesRenderer != null) {
            String statBadge = attributesRenderer.getStatPointsBadge();
            if (statBadge != null) total += Integer.parseInt(statBadge);
        }

        if (classesRenderer != null) {
            String talentBadge = classesRenderer.getTalentPointsBadge();
            if (talentBadge != null) total += Integer.parseInt(talentBadge);
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
                if (attributesRenderer != null) attributesRenderer.renderContent(cmd, events, store, playerRef, "stats");
                else showUnavailable(cmd, "Attributes");
            }
            case "talents" -> {
                if (classesRenderer != null) classesRenderer.renderContent(cmd, events, store, playerRef, "talents");
                else showUnavailable(cmd, "Classes");
            }
            case "allocate" -> {
                if (attributesRenderer != null) attributesRenderer.renderContent(cmd, events, store, playerRef, "allocate");
                else showUnavailable(cmd, "Attributes");
            }
            default -> {
                if (attributesRenderer != null) attributesRenderer.renderContent(cmd, events, store, playerRef, "stats");
                else showUnavailable(cmd, "Attributes");
            }
        }
    }

    /**
     * Handles stat point allocation (delegates to attributes renderer).
     */
    public boolean handleAllocation(PlayerRef playerRef, String actionData) {
        return attributesRenderer != null && attributesRenderer.handleAllocation(playerRef, actionData);
    }

    public String handleTalentAllocation(PlayerRef playerRef, String talentId) {
        return classesRenderer != null ? classesRenderer.handleTalentAllocation(playerRef, talentId) : "Classes not available";
    }

    public int handleTalentReset(PlayerRef playerRef) {
        return classesRenderer != null ? classesRenderer.handleTalentReset(playerRef) : 0;
    }

    private void showUnavailable(UICommandBuilder cmd, String module) {
        cmd.set("#ContentText.Visible", true);
        cmd.set("#ContentText.Text", module + " module not available.");
    }

    private static boolean isClassesAvailable() {
        try {
            Class.forName("com.hcclasses.api.HC_ClassesAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
