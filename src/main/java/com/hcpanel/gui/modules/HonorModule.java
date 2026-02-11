package com.hcpanel.gui.modules;

import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Honor module provider for HC_Panel.
 * Provides access to honor stats, brackets, leaderboard, shop, and ranks.
 */
public class HonorModule implements ModuleContentProvider {

    @Override
    public String getModuleName() {
        return "Honor";
    }

    @Override
    public String getModuleId() {
        return "honor";
    }

    @Override
    public String getHeaderColor() {
        return "#d4af37"; // Gold
    }

    @Override
    public List<SidebarItem> getSidebarItems(PlayerRef playerRef) {
        List<SidebarItem> items = new ArrayList<>();

        items.add(new SidebarItem("my_standing", "My Standing"));
        items.add(new SidebarItem("brackets", "Brackets"));
        items.add(new SidebarItem("honor_shop", "Honor Shop"));
        items.add(new SidebarItem("leaderboard", "Leaderboard"));
        items.add(new SidebarItem("all_ranks", "All Ranks"));

        return items;
    }

    @Override
    public InteractiveCustomUIPage<?> createContentPage(PlayerRef playerRef, String view, Object parentGui) {
        try {
            Class<?> pluginClass = Class.forName("com.hchonor.HC_HonorPlugin");
            var getInstance = pluginClass.getMethod("getInstance");
            var plugin = getInstance.invoke(null);
            if (plugin == null) return null;

            return switch (view) {
                case "my_standing" -> createHonorPanelGui(plugin, playerRef);
                case "brackets" -> createBracketsGui(plugin, playerRef, parentGui);
                case "honor_shop" -> createShopGui(plugin, playerRef, parentGui);
                case "leaderboard" -> createLeaderboardGui(plugin, playerRef);
                case "all_ranks" -> createRanksGui(plugin, playerRef, parentGui);
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getDefaultView() {
        return "my_standing";
    }

    // GUI creation methods using reflection
    // HC_Honor GUIs take InteractiveCustomUIPage<?> as parent parameter

    private InteractiveCustomUIPage<?> createHonorPanelGui(Object plugin, PlayerRef playerRef) {
        try {
            Class<?> guiClass = Class.forName("com.hchonor.gui.HonorPanelGui");
            var constructor = guiClass.getConstructor(plugin.getClass(), PlayerRef.class);
            return (InteractiveCustomUIPage<?>) constructor.newInstance(plugin, playerRef);
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private InteractiveCustomUIPage<?> createBracketsGui(Object plugin, PlayerRef playerRef, Object parentGui) {
        try {
            Class<?> guiClass = Class.forName("com.hchonor.gui.HonorBracketsGui");
            Class<?> parentType = Class.forName("com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage");
            var constructor = guiClass.getConstructor(plugin.getClass(), PlayerRef.class, parentType);
            return (InteractiveCustomUIPage<?>) constructor.newInstance(plugin, playerRef, null);
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private InteractiveCustomUIPage<?> createShopGui(Object plugin, PlayerRef playerRef, Object parentGui) {
        try {
            Class<?> guiClass = Class.forName("com.hchonor.gui.HonorShopGui");
            Class<?> parentType = Class.forName("com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage");
            var constructor = guiClass.getConstructor(plugin.getClass(), PlayerRef.class, parentType);
            return (InteractiveCustomUIPage<?>) constructor.newInstance(plugin, playerRef, null);
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private InteractiveCustomUIPage<?> createLeaderboardGui(Object plugin, PlayerRef playerRef) {
        try {
            Class<?> guiClass = Class.forName("com.hchonor.gui.HonorLeaderboardGui");
            var constructor = guiClass.getConstructor(plugin.getClass(), PlayerRef.class);
            return (InteractiveCustomUIPage<?>) constructor.newInstance(plugin, playerRef);
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private InteractiveCustomUIPage<?> createRanksGui(Object plugin, PlayerRef playerRef, Object parentGui) {
        try {
            Class<?> guiClass = Class.forName("com.hchonor.gui.HonorRanksGui");
            Class<?> parentType = Class.forName("com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage");
            var constructor = guiClass.getConstructor(plugin.getClass(), PlayerRef.class, parentType);
            return (InteractiveCustomUIPage<?>) constructor.newInstance(plugin, playerRef, null);
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
}
