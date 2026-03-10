package com.hcpanel.gui.modules;

import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Recruitment module provider for HC_Panel.
 * Shows rank display, bids on me, browse recruits, and guild bids.
 */
public class RecruitmentModule implements ModuleContentProvider {

    @Override
    public String getModuleName() {
        return "Recruitment";
    }

    @Override
    public String getModuleId() {
        return "recruitment";
    }

    @Override
    public String getHeaderColor() {
        return "#ffd700"; // Gold
    }

    @Override
    public List<SidebarItem> getSidebarItems(PlayerRef playerRef) {
        List<SidebarItem> items = new ArrayList<>();
        items.add(new SidebarItem("rank", "My Rank"));
        items.add(new SidebarItem("bids", "Bids on Me"));
        items.add(new SidebarItem("browse", "Browse Recruits"));
        items.add(new SidebarItem("guildbids", "Guild Bids"));
        return items;
    }

    @Override
    public InteractiveCustomUIPage<?> createContentPage(PlayerRef playerRef, String view, Object parentGui) {
        // Content is rendered via RecruitmentContentRenderer in UnifiedPanelGui
        return null;
    }

    @Override
    public String getDefaultView() {
        return "rank";
    }
}
