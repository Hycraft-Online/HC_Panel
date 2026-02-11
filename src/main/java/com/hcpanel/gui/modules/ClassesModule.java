package com.hcpanel.gui.modules;

import com.hcclasses.api.HC_ClassesAPI;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Classes module provider for HC_Panel.
 * Provides access to talent tree and class information.
 */
public class ClassesModule implements ModuleContentProvider {

    @Override
    public String getModuleName() {
        return "Classes";
    }

    @Override
    public String getModuleId() {
        return "classes";
    }

    @Override
    public String getBadgeText(PlayerRef playerRef) {
        if (!HC_ClassesAPI.isAvailable()) return null;

        int availablePoints = HC_ClassesAPI.getAvailableTalentPoints(playerRef.getUuid());
        return availablePoints > 0 ? String.valueOf(availablePoints) : null;
    }

    @Override
    public String getHeaderColor() {
        return "#8b4513"; // Brown - class color
    }

    @Override
    public List<SidebarItem> getSidebarItems(PlayerRef playerRef) {
        List<SidebarItem> items = new ArrayList<>();

        String badge = getBadgeText(playerRef);
        items.add(new SidebarItem("talents", "Talents", false, badge));

        return items;
    }

    @Override
    public InteractiveCustomUIPage<?> createContentPage(PlayerRef playerRef, String view, Object parentGui) {
        // Content is rendered via ClassesContentRenderer in UnifiedPanelGui
        // This method is not used for the unified panel approach
        return null;
    }

    @Override
    public String getDefaultView() {
        return "talents";
    }
}
