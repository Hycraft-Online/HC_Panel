package com.hcpanel.gui.content;

import com.hcpanel.gui.UnifiedPanelGui.SidebarButton;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Direct imports from HC_Classes (optional dependency)
import com.hcclasses.api.HC_ClassesAPI;
import com.hcclasses.models.PlayerClass;
import com.hcclasses.talents.Talent;
import com.hcclasses.talents.TalentTree;

// Direct imports from HC_Factions (optional dependency)
import com.hcfactions.HC_FactionsPlugin;
import com.hcfactions.models.PlayerData;

import com.hypixel.hytale.server.core.Message;

import java.awt.Color;
import java.util.*;

/**
 * Renders Classes module content into the unified panel.
 * Handles talent tree display with tier-based layout.
 */
public class ClassesContentRenderer {

    private final PlayerRef playerRef;

    // Node states with their colors
    private static final String COLOR_LOCKED_BG = "#2a2a3a";
    private static final String COLOR_LOCKED_TEXT = "#555555";
    private static final String COLOR_AVAILABLE_BG = "#2a4a3a";
    private static final String COLOR_AVAILABLE_TEXT = "#4aff7f";
    private static final String COLOR_MAXED_BG = "#3a3a5a";
    private static final String COLOR_MAXED_TEXT = "#ffd700";

    // Header color for Classes module
    private static final String HEADER_COLOR = "#8b4513";

    public ClassesContentRenderer(PlayerRef playerRef) {
        this.playerRef = playerRef;
    }

    /**
     * Returns sidebar buttons for the Classes module.
     */
    public List<SidebarButton> getSidebarButtons(String currentView) {
        List<SidebarButton> buttons = new ArrayList<>();

        boolean isTalents = currentView == null || "talents".equals(currentView);

        String badge = getTalentPointsBadge();

        buttons.add(new SidebarButton("Talents", "nav:classes:talents", badge, HEADER_COLOR, isTalents));

        return buttons;
    }

    /**
     * Returns a badge string if the player has unspent talent points.
     */
    public String getTalentPointsBadge() {
        if (!HC_ClassesAPI.isAvailable()) return null;

        int points = HC_ClassesAPI.getAvailableTalentPoints(playerRef.getUuid());
        return points > 0 ? String.valueOf(points) : null;
    }

    /**
     * Renders content for the specified view.
     */
    public void renderContent(UICommandBuilder cmd, UIEventBuilder events, Store<EntityStore> store,
                               PlayerRef playerRef, String view) {
        if (!HC_ClassesAPI.isAvailable()) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "Classes system not available.");
            return;
        }

        if (!HC_ClassesAPI.hasChosenClass(playerRef.getUuid())) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "You have not chosen a class yet.\nUse /class to select your class.");
            return;
        }

        // Default to talents view
        if (view == null) view = "talents";

        switch (view) {
            case "talents" -> renderTalentTree(cmd, events);
            default -> renderTalentTree(cmd, events);
        }
    }

    // Maximum number of tiers supported by the UI layout
    private static final int MAX_UI_TIERS = 9;

    private void renderTalentTree(UICommandBuilder cmd, UIEventBuilder events) {
        // Get player's talent tree and allocations
        TalentTree tree = HC_ClassesAPI.getPlayerTalentTree(playerRef.getUuid());
        if (tree == null) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "Talent tree not available for your class.");
            return;
        }

        Map<String, Integer> allocations = HC_ClassesAPI.getPlayerTalentAllocations(playerRef.getUuid());
        int availablePoints = HC_ClassesAPI.getAvailableTalentPoints(playerRef.getUuid());
        int classLevel = HC_ClassesAPI.getClassLevel(playerRef.getUuid());
        int displayLevel = getPlayerLevel();
        PlayerClass playerClass = HC_ClassesAPI.getPlayerClass(playerRef.getUuid());

        // === HEADER SECTION (matches overview/allocate hero header) ===
        cmd.set("#HeaderSection.Visible", false);
        cmd.set("#AttrHeroSection.Visible", true);

        cmd.set("#AttrLevelNumber.Text", String.valueOf(displayLevel));

        String classDisplay = playerClass != null ? playerClass.getDisplayName() : "Adventurer";
        cmd.set("#AttrClassName.Text", classDisplay.toUpperCase());
        cmd.set("#AttrPlayerName.Text", playerRef.getUsername());

        // Faction tag
        String factionId = getFactionId();
        if (factionId != null) {
            var factionsPlugin = HC_FactionsPlugin.getInstance();
            if (factionsPlugin != null) {
                var faction = factionsPlugin.getFactionManager().getFaction(factionId);
                if (faction != null) {
                    cmd.set("#AttrFactionTag.TextSpans",
                        Message.raw("[" + faction.getShortName() + "]").color(Color.decode(faction.getColorHex())));
                }
            }
        }

        // Available points banner
        if (availablePoints > 0) {
            cmd.set("#AttrPointsBanner.Visible", true);
            cmd.set("#AttrPointsValue.Text", String.valueOf(availablePoints));
        }

        // === TALENT TREE SECTION ===
        cmd.set("#TalentTreeSection.Visible", true);
        cmd.set("#ContentSpacer.Visible", false);

        // Points display row
        int totalSpent = allocations.values().stream().mapToInt(Integer::intValue).sum();
        cmd.set("#TalentPointsAvailable.Text", String.valueOf(availablePoints));
        cmd.set("#TalentPointsSpent.Text", String.valueOf(totalSpent));

        // Reset button event
        events.addEventBinding(CustomUIEventBindingType.Activating, "#TalentResetBtn",
            EventData.of("Action", "action:talent:reset:all"), false);

        // Render all tiers — nothing is hidden, just locked/disabled
        int tierCount = Math.min(tree.getTierCount(), MAX_UI_TIERS);
        for (int tier = 0; tier < tierCount; tier++) {
            List<Talent> tierTalents = tree.getTalentsAtTier(tier);
            renderTier(cmd, events, tier, tierTalents, allocations, availablePoints, classLevel, tree);
        }

        // Footer
        cmd.set("#FooterText.Text", "Click a talent to spend points. Reset refunds all allocated points.");
    }

    // Max node slots per tier: 4 for tiers 0-3 (single row), 8 for tiers 4+ (two rows)
    private static final int MAX_NODES_CORE = 4;
    private static final int MAX_NODES_BRANCH = 8;

    private void renderTier(UICommandBuilder cmd, UIEventBuilder events, int tier,
                           List<Talent> talents, Map<String, Integer> allocations,
                           int availablePoints, int playerLevel, TalentTree tree) {
        String tierPrefix = "#TalentTier" + tier;
        int maxSlots = tier >= 4 ? MAX_NODES_BRANCH : MAX_NODES_CORE;

        // Render all node slots
        for (int slot = 0; slot < maxSlots; slot++) {
            String wrapPrefix = tierPrefix + "Node" + slot + "Wrap";
            String btnPrefix = tierPrefix + "Node" + slot + "Btn";
            String rankId = tierPrefix + "Node" + slot + "Rank";

            if (slot < talents.size()) {
                Talent talent = talents.get(slot);
                renderTalentNode(cmd, events, wrapPrefix, btnPrefix, rankId, talent, allocations, availablePoints, playerLevel, tree);
            } else {
                // Hide unused slot
                cmd.set(wrapPrefix + ".Visible", false);
            }
        }

        // For tiers 4+, show/hide RowB based on whether we need > 4 slots
        if (tier >= 4) {
            cmd.set(tierPrefix + "RowB.Visible", talents.size() > 4);
        }
    }

    private void renderTalentNode(UICommandBuilder cmd, UIEventBuilder events,
                                  String wrapPrefix, String btnPrefix, String rankId,
                                  Talent talent, Map<String, Integer> allocations,
                                  int availablePoints, int playerLevel, TalentTree tree) {
        int currentRank = allocations.getOrDefault(talent.getId(), 0);
        int maxRank = talent.getMaxRank();

        // Determine node state
        NodeState state = calculateNodeState(talent, currentRank, allocations, availablePoints, playerLevel, tree);

        cmd.set(wrapPrefix + ".Visible", true);

        // Choose text color based on state
        Color textColorObj = switch (state) {
            case LOCKED -> Color.decode(COLOR_LOCKED_TEXT);
            case AVAILABLE -> Color.decode(COLOR_AVAILABLE_TEXT);
            case MAXED -> Color.decode(COLOR_MAXED_TEXT);
        };

        // Set button background via .Background (buttons support this directly)
        String bgColor = switch (state) {
            case LOCKED -> COLOR_LOCKED_BG;
            case AVAILABLE -> COLOR_AVAILABLE_BG;
            case MAXED -> COLOR_MAXED_BG;
        };
        cmd.set(btnPrefix + ".Background", bgColor);

        // Set talent abbreviation via .TextSpans with color
        String abbrev = talent.getName().length() > 3
            ? talent.getName().substring(0, 3).toUpperCase()
            : talent.getName().toUpperCase();
        cmd.set(btnPrefix + ".TextSpans", Message.raw(abbrev).color(textColorObj));

        // Set rank display
        String rankText = currentRank + "/" + maxRank;
        cmd.set(rankId + ".TextSpans", Message.raw(rankText).color(textColorObj));

        // Build tooltip (set on wrapper for hover area)
        String tooltip = buildTooltip(talent, currentRank, state, allocations, playerLevel, tree);
        cmd.set(wrapPrefix + ".TooltipText", tooltip);

        // Bind click event - always bind but only allocate if available
        events.addEventBinding(CustomUIEventBindingType.Activating, btnPrefix,
            EventData.of("Action", "action:talent:allocate:" + talent.getId()), false);
    }

    private NodeState calculateNodeState(Talent talent, int currentRank, Map<String, Integer> allocations,
                                         int availablePoints, int playerLevel, TalentTree tree) {
        // Already maxed?
        if (currentRank >= talent.getMaxRank()) {
            return NodeState.MAXED;
        }

        // Check if can learn
        String error = tree.canLearnTalent(talent.getId(), allocations, playerLevel);
        if (error != null || availablePoints <= 0) {
            return NodeState.LOCKED;
        }

        return NodeState.AVAILABLE;
    }

    private String buildTooltip(Talent talent, int currentRank, NodeState state,
                                Map<String, Integer> allocations, int playerLevel, TalentTree tree) {
        StringBuilder sb = new StringBuilder();
        sb.append(talent.getName()).append("\n");
        sb.append(talent.getDescription()).append("\n\n");
        sb.append("Rank: ").append(currentRank).append("/").append(talent.getMaxRank()).append("\n");
        sb.append("Required Level: ").append(talent.getRequiredLevel());

        // Show prerequisites if any
        if (!talent.getPrerequisites().isEmpty()) {
            sb.append("\n\nRequires:");
            for (String prereqId : talent.getPrerequisites()) {
                Talent prereq = tree.getTalent(prereqId);
                if (prereq != null) {
                    int prereqRank = allocations.getOrDefault(prereqId, 0);
                    String status = prereqRank >= prereq.getMaxRank() ? " [OK]" : " [" + prereqRank + "/" + prereq.getMaxRank() + "]";
                    sb.append("\n  - ").append(prereq.getName()).append(status);
                }
            }
        }

        // Show reason if locked
        if (state == NodeState.LOCKED) {
            String error = tree.canLearnTalent(talent.getId(), allocations, playerLevel);
            if (error != null) {
                sb.append("\n\n[LOCKED: ").append(error).append("]");
            }
        }

        return sb.toString();
    }

    /**
     * Handles talent allocation.
     * @return error message on failure, null on success
     */
    public String handleTalentAllocation(PlayerRef playerRef, String talentId) {
        if (!HC_ClassesAPI.isAvailable()) return "Classes system not available";

        return HC_ClassesAPI.allocateTalent(playerRef, talentId);
    }

    /**
     * Handles talent reset.
     * @return number of points refunded
     */
    public int handleTalentReset(PlayerRef playerRef) {
        if (!HC_ClassesAPI.isAvailable()) return 0;

        return HC_ClassesAPI.resetTalents(playerRef);
    }

    private int getPlayerLevel() {
        try {
            Class<?> apiClass = Class.forName("com.hcleveling.api.HC_LevelingAPI");
            var isAvailableMethod = apiClass.getMethod("isAvailable");
            Boolean isAvailable = (Boolean) isAvailableMethod.invoke(null);
            if (!isAvailable) return 1;

            var getLevelMethod = apiClass.getMethod("getPlayerLevel", java.util.UUID.class);
            return (Integer) getLevelMethod.invoke(null, playerRef.getUuid());
        } catch (Exception e) {
            return 1;
        }
    }

    private String getFactionId() {
        HC_FactionsPlugin factionsPlugin = HC_FactionsPlugin.getInstance();
        if (factionsPlugin == null) return null;

        PlayerData playerData = factionsPlugin.getPlayerDataRepository().getPlayerData(playerRef.getUuid());
        return playerData != null ? playerData.getFactionId() : null;
    }

    private enum NodeState {
        LOCKED,
        AVAILABLE,
        MAXED
    }
}
