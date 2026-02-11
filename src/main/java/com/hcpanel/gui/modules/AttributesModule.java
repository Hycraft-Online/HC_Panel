package com.hcpanel.gui.modules;

import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Attributes module provider for HC_Panel.
 * Provides access to character stats, stat allocation, and class info.
 */
public class AttributesModule implements ModuleContentProvider {

    @Override
    public String getModuleName() {
        return "Attributes";
    }

    @Override
    public String getModuleId() {
        return "attributes";
    }

    @Override
    public String getBadgeText(PlayerRef playerRef) {
        // Show available stat points if any
        int points = getAvailablePoints(playerRef);
        return points > 0 ? String.valueOf(points) : null;
    }

    @Override
    public String getHeaderColor() {
        return "#8b5cf6"; // Purple
    }

    @Override
    public List<SidebarItem> getSidebarItems(PlayerRef playerRef) {
        List<SidebarItem> items = new ArrayList<>();

        int availablePoints = getAvailablePoints(playerRef);

        items.add(new SidebarItem("my_stats", "My Stats"));
        items.add(new SidebarItem("allocate", "Allocate", false,
            availablePoints > 0 ? String.valueOf(availablePoints) : null));
        items.add(new SidebarItem("class_info", "Class Info"));

        return items;
    }

    @Override
    public InteractiveCustomUIPage<?> createContentPage(PlayerRef playerRef, String view, Object parentGui) {
        try {
            Class<?> pluginClass = Class.forName("com.hcattributes.HC_AttributesPlugin");
            var getInstance = pluginClass.getMethod("getInstance");
            var plugin = getInstance.invoke(null);
            if (plugin == null) return null;

            return switch (view) {
                case "my_stats" -> createCharacterPanelGui(plugin, playerRef);
                case "allocate" -> createAllocatePanelGui(plugin, playerRef, parentGui);
                case "class_info" -> createCharacterPanelGui(plugin, playerRef); // Reuse stats panel for now
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getDefaultView() {
        return "my_stats";
    }

    // Helper method to get available stat points

    private int getAvailablePoints(PlayerRef playerRef) {
        try {
            Class<?> apiClass = Class.forName("com.hcattributes.api.HC_AttributesAPI");
            var getPoints = apiClass.getMethod("getAvailableStatPoints", java.util.UUID.class);
            return (int) getPoints.invoke(null, playerRef.getUuid());
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }

    // GUI creation methods using reflection
    // HC_Attributes GUIs take InteractiveCustomUIPage<?> as parent parameter

    private InteractiveCustomUIPage<?> createCharacterPanelGui(Object plugin, PlayerRef playerRef) {
        try {
            Class<?> guiClass = Class.forName("com.hcattributes.gui.CharacterPanelGui");
            var constructor = guiClass.getConstructor(plugin.getClass(), PlayerRef.class);
            return (InteractiveCustomUIPage<?>) constructor.newInstance(plugin, playerRef);
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private InteractiveCustomUIPage<?> createAllocatePanelGui(Object plugin, PlayerRef playerRef, Object parentGui) {
        try {
            Class<?> guiClass = Class.forName("com.hcattributes.gui.AllocatePanelGui");
            Class<?> parentType = Class.forName("com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage");
            var constructor = guiClass.getConstructor(plugin.getClass(), PlayerRef.class, parentType);
            return (InteractiveCustomUIPage<?>) constructor.newInstance(plugin, playerRef, null);
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
}
