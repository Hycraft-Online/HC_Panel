package com.hcpanel.gui.modules;

import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Skills module provider for HC_Panel.
 * Shows Professions and Tradeskills progress with levels and XP bars.
 */
public class SkillsModule implements ModuleContentProvider {

    @Override
    public String getModuleName() {
        return "Skills";
    }

    @Override
    public String getModuleId() {
        return "skills";
    }

    @Override
    public String getHeaderColor() {
        return "#4ecdc4"; // Teal
    }

    @Override
    public List<SidebarItem> getSidebarItems(PlayerRef playerRef) {
        List<SidebarItem> items = new ArrayList<>();
        items.add(new SidebarItem("professions", "Professions"));
        items.add(new SidebarItem("tradeskills", "Tradeskills"));
        return items;
    }

    @Override
    public InteractiveCustomUIPage<?> createContentPage(PlayerRef playerRef, String view, Object parentGui) {
        // Content is rendered via SkillsContentRenderer in UnifiedPanelGui
        return null;
    }

    @Override
    public String getDefaultView() {
        return "professions";
    }
}
