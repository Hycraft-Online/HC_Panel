package com.hcpanel.gui.modules;

import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.List;

/**
 * Characters module provider for HC_Panel.
 * Allows switching between multiple characters via HC_MultiChar.
 */
public class CharactersModule implements ModuleContentProvider {

    @Override
    public String getModuleName() {
        return "Characters";
    }

    @Override
    public String getModuleId() {
        return "characters";
    }

    @Override
    public String getHeaderColor() {
        return "#22d3ee"; // Cyan accent
    }

    @Override
    public List<SidebarItem> getSidebarItems(PlayerRef playerRef) {
        return List.of(new SidebarItem("select", "Switch Character"));
    }

    @Override
    public InteractiveCustomUIPage<?> createContentPage(PlayerRef playerRef, String view, Object parentGui) {
        // Handled via direct GUI open in MainMenuGui
        return null;
    }

    @Override
    public String getDefaultView() {
        return "select";
    }
}
