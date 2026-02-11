package com.hcpanel.gui.modules;

import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Character module provider for HC_Panel.
 * Combines Attributes and Classes into a unified character panel.
 * Subpages: Overview (stats), Talents, Allocate
 */
public class CharacterModule implements ModuleContentProvider {

    @Override
    public String getModuleName() {
        return "Character";
    }

    @Override
    public String getModuleId() {
        return "character";
    }

    @Override
    public String getBadgeText(PlayerRef playerRef) {
        // Show combined unspent points (stat points + talent points)
        int totalPoints = 0;

        // Get stat points from HC_Attributes
        totalPoints += getAvailableStatPoints(playerRef);

        // Get talent points from HC_Classes
        totalPoints += getAvailableTalentPoints(playerRef);

        return totalPoints > 0 ? String.valueOf(totalPoints) : null;
    }

    @Override
    public String getHeaderColor() {
        return "#d4af37"; // Gold - unified character color
    }

    @Override
    public List<SidebarItem> getSidebarItems(PlayerRef playerRef) {
        List<SidebarItem> items = new ArrayList<>();

        int statPoints = getAvailableStatPoints(playerRef);
        int talentPoints = getAvailableTalentPoints(playerRef);

        // Overview (was "My Stats") - no badge
        items.add(new SidebarItem("overview", "Overview"));

        // Talents - show talent points badge if any
        items.add(new SidebarItem("talents", "Talents", false,
            talentPoints > 0 ? String.valueOf(talentPoints) : null));

        // Allocate - show stat points badge if any
        items.add(new SidebarItem("allocate", "Allocate", false,
            statPoints > 0 ? String.valueOf(statPoints) : null));

        return items;
    }

    @Override
    public InteractiveCustomUIPage<?> createContentPage(PlayerRef playerRef, String view, Object parentGui) {
        // Content is rendered via CharacterContentRenderer in UnifiedPanelGui
        return null;
    }

    @Override
    public String getDefaultView() {
        return "overview";
    }

    // Helper methods using reflection to avoid compile-time dependencies

    private int getAvailableStatPoints(PlayerRef playerRef) {
        try {
            Class<?> apiClass = Class.forName("com.hcattributes.api.HC_AttributesAPI");
            var isAvailable = apiClass.getMethod("isAvailable");
            if (!(Boolean) isAvailable.invoke(null)) return 0;

            var getPoints = apiClass.getMethod("getAvailableStatPoints", java.util.UUID.class);
            return (int) getPoints.invoke(null, playerRef.getUuid());
        } catch (Exception e) {
            return 0;
        }
    }

    private int getAvailableTalentPoints(PlayerRef playerRef) {
        try {
            Class<?> apiClass = Class.forName("com.hcclasses.api.HC_ClassesAPI");
            var isAvailable = apiClass.getMethod("isAvailable");
            if (!(Boolean) isAvailable.invoke(null)) return 0;

            var getPoints = apiClass.getMethod("getAvailableTalentPoints", java.util.UUID.class);
            return (int) getPoints.invoke(null, playerRef.getUuid());
        } catch (Exception e) {
            return 0;
        }
    }
}
