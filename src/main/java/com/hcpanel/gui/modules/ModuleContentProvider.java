package com.hcpanel.gui.modules;

import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.List;

/**
 * Interface for module content providers.
 * Each module (Factions, Honor, Attributes) implements this to provide
 * its sidebar items and content pages.
 */
public interface ModuleContentProvider {

    /**
     * Returns the module name displayed in the main menu sidebar.
     * Example: "Factions", "Honor", "Attributes"
     */
    String getModuleName();

    /**
     * Returns the module ID used for internal identification.
     * Example: "factions", "honor", "attributes"
     */
    String getModuleId();

    /**
     * Returns the icon/badge text to display next to the module name (optional).
     * Return null for no badge, or a number string like "3" for pending items.
     */
    default String getBadgeText(PlayerRef playerRef) {
        return null;
    }

    /**
     * Returns the color hex for the module header.
     * Example: "#d4af37" for gold, "#4169e1" for blue
     */
    String getHeaderColor();

    /**
     * Returns the list of sidebar items for this module.
     * Each item becomes a button in the module's sidebar.
     */
    List<SidebarItem> getSidebarItems(PlayerRef playerRef);

    /**
     * Creates the content page for the given view.
     * The view parameter matches a SidebarItem.viewId.
     *
     * @param playerRef The player viewing the module
     * @param view The view ID to display (from SidebarItem.viewId)
     * @param parentGui The parent menu GUI for navigation back
     * @return The content page to display
     */
    InteractiveCustomUIPage<?> createContentPage(PlayerRef playerRef, String view, Object parentGui);

    /**
     * Returns the default view to show when the module is first opened.
     * Should match one of the SidebarItem.viewId values.
     */
    String getDefaultView();

    /**
     * Represents a sidebar navigation item within a module.
     */
    record SidebarItem(
        String viewId,           // Internal ID for this view
        String displayName,      // Text shown on the button
        boolean officerOnly,     // Only show if player has officer+ role
        String badgeText         // Optional badge (null for none)
    ) {
        public SidebarItem(String viewId, String displayName) {
            this(viewId, displayName, false, null);
        }

        public SidebarItem(String viewId, String displayName, boolean officerOnly) {
            this(viewId, displayName, officerOnly, null);
        }
    }
}
