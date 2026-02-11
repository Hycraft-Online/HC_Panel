package com.hcpanel.gui.modules;

import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Factions module provider for HC_Panel.
 * Provides access to guild management, faction overview, and land claims.
 */
public class FactionsModule implements ModuleContentProvider {

    @Override
    public String getModuleName() {
        return "Factions";
    }

    @Override
    public String getModuleId() {
        return "factions";
    }

    @Override
    public String getBadgeText(PlayerRef playerRef) {
        // Show pending invitation count
        try {
            Class<?> pluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
            var getInstance = pluginClass.getMethod("getInstance");
            var plugin = getInstance.invoke(null);
            if (plugin != null) {
                var getManager = pluginClass.getMethod("getGuildManager");
                var manager = getManager.invoke(plugin);
                if (manager != null) {
                    var getCount = manager.getClass().getMethod("getPendingInvitationCount", java.util.UUID.class);
                    int count = (int) getCount.invoke(manager, playerRef.getUuid());
                    return count > 0 ? String.valueOf(count) : null;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    @Override
    public String getHeaderColor() {
        return "#4a9eff"; // Blue
    }

    @Override
    public List<SidebarItem> getSidebarItems(PlayerRef playerRef) {
        List<SidebarItem> items = new ArrayList<>();

        boolean hasGuild = playerHasGuild(playerRef);
        boolean isOfficer = playerIsOfficer(playerRef);
        int inviteCount = getInvitationCount(playerRef);

        if (hasGuild) {
            items.add(new SidebarItem("guild_info", "My Guild"));
        }

        if (hasGuild && isOfficer) {
            items.add(new SidebarItem("guild_management", "Manage Members", true));
        }

        items.add(new SidebarItem("guild_browser", "Browse Guilds"));
        items.add(new SidebarItem("invitations", "Invitations", false,
            inviteCount > 0 ? String.valueOf(inviteCount) : null));
        items.add(new SidebarItem("faction_overview", "Faction Overview"));

        if (hasGuild && isOfficer) {
            items.add(new SidebarItem("land_claims", "Land Claims", true));
        }

        return items;
    }

    @Override
    public InteractiveCustomUIPage<?> createContentPage(PlayerRef playerRef, String view, Object parentGui) {
        try {
            Class<?> pluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
            var getInstance = pluginClass.getMethod("getInstance");
            var plugin = getInstance.invoke(null);
            if (plugin == null) return null;

            return switch (view) {
                case "guild_info" -> createGuildInfoGui(plugin, playerRef, parentGui);
                case "guild_management" -> createGuildManagementGui(plugin, playerRef, parentGui);
                case "guild_browser" -> createGuildBrowserGui(plugin, playerRef, parentGui);
                case "invitations" -> createInvitationsGui(plugin, playerRef, parentGui);
                case "faction_overview" -> createFactionOverviewGui(plugin, playerRef, parentGui);
                case "land_claims" -> createLandClaimsGui(plugin, playerRef);
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getDefaultView() {
        return "guild_browser"; // Default to browser if no guild
    }

    // Helper methods for player state checks

    private boolean playerHasGuild(PlayerRef playerRef) {
        try {
            Class<?> pluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
            var getInstance = pluginClass.getMethod("getInstance");
            var plugin = getInstance.invoke(null);
            if (plugin != null) {
                var getRepo = pluginClass.getMethod("getPlayerDataRepository");
                var repo = getRepo.invoke(plugin);
                if (repo != null) {
                    var getData = repo.getClass().getMethod("getPlayerData", java.util.UUID.class);
                    var data = getData.invoke(repo, playerRef.getUuid());
                    if (data != null) {
                        var isInGuild = data.getClass().getMethod("isInGuild");
                        return (boolean) isInGuild.invoke(data);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    private boolean playerIsOfficer(PlayerRef playerRef) {
        try {
            Class<?> pluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
            var getInstance = pluginClass.getMethod("getInstance");
            var plugin = getInstance.invoke(null);
            if (plugin != null) {
                var getRepo = pluginClass.getMethod("getPlayerDataRepository");
                var repo = getRepo.invoke(plugin);
                if (repo != null) {
                    var getData = repo.getClass().getMethod("getPlayerData", java.util.UUID.class);
                    var data = getData.invoke(repo, playerRef.getUuid());
                    if (data != null) {
                        var getRole = data.getClass().getMethod("getGuildRole");
                        var role = getRole.invoke(data);
                        if (role != null) {
                            // GuildRole.OFFICER or LEADER
                            var hasAtLeast = role.getClass().getMethod("hasAtLeast",
                                Class.forName("com.hcfactions.models.GuildRole"));
                            var officerRole = Class.forName("com.hcfactions.models.GuildRole")
                                .getField("OFFICER").get(null);
                            return (boolean) hasAtLeast.invoke(role, officerRole);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    private int getInvitationCount(PlayerRef playerRef) {
        try {
            Class<?> pluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
            var getInstance = pluginClass.getMethod("getInstance");
            var plugin = getInstance.invoke(null);
            if (plugin != null) {
                var getManager = pluginClass.getMethod("getGuildManager");
                var manager = getManager.invoke(plugin);
                if (manager != null) {
                    var getCount = manager.getClass().getMethod("getPendingInvitationCount", java.util.UUID.class);
                    return (int) getCount.invoke(manager, playerRef.getUuid());
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }

    // GUI creation methods using reflection
    // HC_Factions GUIs take InteractiveCustomUIPage<?> as parent parameter

    private InteractiveCustomUIPage<?> createGuildInfoGui(Object plugin, PlayerRef playerRef, Object parentGui) {
        try {
            Class<?> guiClass = Class.forName("com.hcfactions.gui.GuildInfoGui");
            Class<?> parentType = Class.forName("com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage");
            var constructor = guiClass.getConstructor(plugin.getClass(), PlayerRef.class, parentType);
            return (InteractiveCustomUIPage<?>) constructor.newInstance(plugin, playerRef, null);
        } catch (Exception e) {
            // Ignore - GUI may not exist or have different signature
        }
        return null;
    }

    private InteractiveCustomUIPage<?> createGuildManagementGui(Object plugin, PlayerRef playerRef, Object parentGui) {
        try {
            Class<?> guiClass = Class.forName("com.hcfactions.gui.GuildManagementGui");
            Class<?> parentType = Class.forName("com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage");
            var constructor = guiClass.getConstructor(plugin.getClass(), PlayerRef.class, parentType);
            return (InteractiveCustomUIPage<?>) constructor.newInstance(plugin, playerRef, null);
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private InteractiveCustomUIPage<?> createGuildBrowserGui(Object plugin, PlayerRef playerRef, Object parentGui) {
        try {
            Class<?> guiClass = Class.forName("com.hcfactions.gui.GuildBrowserGui");
            Class<?> parentType = Class.forName("com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage");
            var constructor = guiClass.getConstructor(plugin.getClass(), PlayerRef.class, parentType);
            return (InteractiveCustomUIPage<?>) constructor.newInstance(plugin, playerRef, null);
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private InteractiveCustomUIPage<?> createInvitationsGui(Object plugin, PlayerRef playerRef, Object parentGui) {
        try {
            Class<?> guiClass = Class.forName("com.hcfactions.gui.InvitationsGui");
            Class<?> parentType = Class.forName("com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage");
            var constructor = guiClass.getConstructor(plugin.getClass(), PlayerRef.class, parentType);
            return (InteractiveCustomUIPage<?>) constructor.newInstance(plugin, playerRef, null);
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private InteractiveCustomUIPage<?> createFactionOverviewGui(Object plugin, PlayerRef playerRef, Object parentGui) {
        try {
            Class<?> guiClass = Class.forName("com.hcfactions.gui.FactionOverviewGui");
            Class<?> parentType = Class.forName("com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage");
            var constructor = guiClass.getConstructor(plugin.getClass(), PlayerRef.class, parentType);
            return (InteractiveCustomUIPage<?>) constructor.newInstance(plugin, playerRef, null);
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private InteractiveCustomUIPage<?> createLandClaimsGui(Object plugin, PlayerRef playerRef) {
        // Land claims requires player position context - return null to indicate
        // it should be opened via special handling (opens full map)
        return null;
    }
}
