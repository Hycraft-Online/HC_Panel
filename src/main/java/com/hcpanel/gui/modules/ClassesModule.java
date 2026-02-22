package com.hcpanel.gui.modules;

import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Classes module provider for HC_Panel.
 * Uses reflection to avoid hard dependency on HC_Classes.
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
        try {
            Class<?> api = Class.forName("com.hcclasses.api.HC_ClassesAPI");
            boolean available = (boolean) api.getMethod("isAvailable").invoke(null);
            if (!available) return null;
            int points = (int) api.getMethod("getAvailableTalentPoints", java.util.UUID.class)
                .invoke(null, playerRef.getUuid());
            return points > 0 ? String.valueOf(points) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getHeaderColor() {
        return "#8b4513";
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
        return null;
    }

    @Override
    public String getDefaultView() {
        return "talents";
    }
}
