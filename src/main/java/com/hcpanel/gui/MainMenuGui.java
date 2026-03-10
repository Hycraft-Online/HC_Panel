package com.hcpanel.gui;

import com.hcpanel.HC_PanelPlugin;
import com.hcpanel.gui.modules.ModuleContentProvider;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Main menu GUI using native Hytale UI.
 * Left sidebar with player info, right area with tile grid for modules.
 */
public class MainMenuGui extends InteractiveCustomUIPage<MainMenuGui.MenuEventData> {

    private static final Map<String, String> MODULE_DESCRIPTIONS = Map.ofEntries(
        Map.entry("factions",    "View your guild, faction standings, and land claims."),
        Map.entry("honor",       "PvP rankings, honor brackets, and combat stats."),
        Map.entry("character",   "Attributes, class talents, and stat allocations."),
        Map.entry("skills",      "Crafting professions, recipes, and skill levels."),
        Map.entry("recruitment", "Find and recruit new members for your guild."),
        Map.entry("characters",  "Switch between your characters or create a new one.")
    );

    private final HC_PanelPlugin plugin;

    public MainMenuGui(HC_PanelPlugin plugin, PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, MenuEventData.CODEC);
        this.plugin = plugin;
    }

    public static void openMenu(HC_PanelPlugin plugin, PlayerRef playerRef, Store<EntityStore> store) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) return;
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        MainMenuGui menu = new MainMenuGui(plugin, playerRef);
        player.getPageManager().openCustomPage(ref, store, menu);
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder cmd,
                     @NonNullDecl UIEventBuilder events, @NonNullDecl Store<EntityStore> store) {
        cmd.append("Pages/HC_Panel_MainMenu.ui");

        UUID uuid = playerRef.getUuid();
        String username = playerRef.getUsername();

        // Player info
        if (username != null && !username.isEmpty()) {
            cmd.set("#AvatarInitial.Text", String.valueOf(username.charAt(0)));
            cmd.set("#UsernameLabel.Text", username);
        }

        String subtitle = getPlayerSubtitle(uuid);
        if (subtitle != null && !subtitle.isEmpty()) {
            cmd.set("#SubtitleLabel.Text", subtitle);
        }

        // Stats
        cmd.set("#LevelValue.Text", orDefault(getPlayerLevel(uuid), "\u2014"));
        cmd.set("#ClassValue.Text", orDefault(getClassName(uuid), "None"));
        cmd.set("#FactionValue.Text", orDefault(getFactionName(uuid), "None"));
        cmd.set("#GuildValue.Text", orDefault(getGuildName(uuid), "None"));
        cmd.set("#OnlineValue.Text", String.valueOf(getOnlineCount()));

        // Populate tiles with available modules
        List<ModuleContentProvider> modules = plugin.getAvailableModules();
        int tileIndex = 0;
        for (ModuleContentProvider module : modules) {
            if (tileIndex >= 9) break;

            cmd.set("#TileTitle" + tileIndex + ".Text", module.getModuleName());
            String desc = MODULE_DESCRIPTIONS.getOrDefault(module.getModuleId(), "");
            cmd.set("#TileDesc" + tileIndex + ".Text", desc);

            events.addEventBinding(CustomUIEventBindingType.Activating, "#TileBtn" + tileIndex,
                EventData.of("Action", "open:" + module.getModuleId()), false);

            tileIndex++;
        }

        // Hide unused slots
        for (int i = tileIndex; i < 9; i++) {
            cmd.set("#Slot" + i + ".Visible", false);
        }

        // Hide empty rows
        if (tileIndex <= 3) {
            cmd.set("#TileRow1.Visible", false);
            cmd.set("#TileRow2.Visible", false);
        } else if (tileIndex <= 6) {
            cmd.set("#TileRow2.Visible", false);
        }
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store,
                               @NonNullDecl MenuEventData data) {
        super.handleDataEvent(ref, store, data);

        if (data.action != null && data.action.startsWith("open:")) {
            String moduleId = data.action.substring(5);
            openModule(ref, store, moduleId);
        }
    }

    private void openModule(Ref<EntityStore> ref, Store<EntityStore> store, String moduleId) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        // Characters module opens HC_MultiChar's GUI directly via reflection
        if ("characters".equals(moduleId)) {
            openCharacterSelect(ref, store, player);
            return;
        }

        // Map module IDs to UnifiedPanelGui navigation paths
        String path = switch (moduleId) {
            case "factions" -> "factions:guild_browser";
            case "honor" -> "honor:standing";
            case "character" -> "character:overview";
            case "skills" -> "skills:professions";
            case "recruitment" -> "recruitment:rank";
            default -> moduleId;
        };

        player.getPageManager().openCustomPage(ref, store,
            new UnifiedPanelGui(plugin, playerRef, path, null));
    }

    private void openCharacterSelect(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        try {
            Class<?> pluginClass = Class.forName("com.hcmultichar.HC_MultiCharPlugin");
            var getInstance = pluginClass.getMethod("getInstance");
            Object multiCharPlugin = getInstance.invoke(null);
            if (multiCharPlugin == null) return;

            Class<?> guiClass = Class.forName("com.hcmultichar.gui.CharacterSelectGui");
            var constructor = guiClass.getConstructor(pluginClass, PlayerRef.class);
            var gui = constructor.newInstance(multiCharPlugin, playerRef);

            player.getPageManager().openCustomPage(ref, store,
                (com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage<?>) gui);
        } catch (Exception e) {
            playerRef.sendMessage(Message.raw("Character selection unavailable.").color(java.awt.Color.RED));
        }
    }

    private static String orDefault(String value, String def) {
        return value != null ? value : def;
    }

    // ─── Event Data ───

    public static class MenuEventData {
        public static final BuilderCodec<MenuEventData> CODEC =
            BuilderCodec.builder(MenuEventData.class, MenuEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (d, v) -> d.action = v, d -> d.action)
                .add()
                .build();

        private String action;
    }

    // ─── Player Data Helpers (reflection-based for optional plugin dependencies) ───

    private static String getPlayerSubtitle(UUID uuid) {
        String tag = getFactionTag(uuid);
        String cls = getClassName(uuid);
        if (tag != null && cls != null) return "[" + tag + "] " + cls;
        if (tag != null) return "[" + tag + "]";
        if (cls != null) return cls;
        return "";
    }

    private static String getPlayerLevel(UUID uuid) {
        try {
            Class<?> apiClass = Class.forName("com.hcleveling.api.HC_LevelingAPI");
            var getInstance = apiClass.getMethod("getInstance");
            var api = getInstance.invoke(null);
            if (api != null) {
                var getLevel = apiClass.getMethod("getPlayerLevel", UUID.class);
                Object level = getLevel.invoke(api, uuid);
                return String.valueOf(level);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String getClassName(UUID uuid) {
        try {
            Class<?> apiClass = Class.forName("com.hcclasses.api.HC_ClassesAPI");
            var getInstance = apiClass.getMethod("getInstance");
            var api = getInstance.invoke(null);
            if (api != null) {
                var getClassName = apiClass.getMethod("getPlayerClassName", UUID.class);
                return (String) getClassName.invoke(api, uuid);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String getFactionTag(UUID uuid) {
        try {
            Class<?> pluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
            var getInstance = pluginClass.getMethod("getInstance");
            var plugin = getInstance.invoke(null);
            if (plugin != null) {
                var getRepo = pluginClass.getMethod("getPlayerDataRepository");
                var repo = getRepo.invoke(plugin);
                var getData = repo.getClass().getMethod("getPlayerData", UUID.class);
                var data = getData.invoke(repo, uuid);
                if (data != null) {
                    var getFactionId = data.getClass().getMethod("getFactionId");
                    String factionId = (String) getFactionId.invoke(data);
                    if (factionId != null) {
                        var getManager = pluginClass.getMethod("getFactionManager");
                        var manager = getManager.invoke(plugin);
                        var getFaction = manager.getClass().getMethod("getFaction", String.class);
                        var faction = getFaction.invoke(manager, factionId);
                        if (faction != null) {
                            var getShortName = faction.getClass().getMethod("getShortName");
                            return (String) getShortName.invoke(faction);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String getFactionName(UUID uuid) {
        try {
            Class<?> pluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
            var getInstance = pluginClass.getMethod("getInstance");
            var plugin = getInstance.invoke(null);
            if (plugin != null) {
                var getRepo = pluginClass.getMethod("getPlayerDataRepository");
                var repo = getRepo.invoke(plugin);
                var getData = repo.getClass().getMethod("getPlayerData", UUID.class);
                var data = getData.invoke(repo, uuid);
                if (data != null) {
                    var getFactionId = data.getClass().getMethod("getFactionId");
                    String factionId = (String) getFactionId.invoke(data);
                    if (factionId != null) {
                        var getManager = pluginClass.getMethod("getFactionManager");
                        var manager = getManager.invoke(plugin);
                        var getFaction = manager.getClass().getMethod("getFaction", String.class);
                        var faction = getFaction.invoke(manager, factionId);
                        if (faction != null) {
                            var getName = faction.getClass().getMethod("getDisplayName");
                            return (String) getName.invoke(faction);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String getGuildName(UUID uuid) {
        try {
            Class<?> pluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
            var getInstance = pluginClass.getMethod("getInstance");
            var plugin = getInstance.invoke(null);
            if (plugin != null) {
                var getRepo = pluginClass.getMethod("getPlayerDataRepository");
                var repo = getRepo.invoke(plugin);
                var getData = repo.getClass().getMethod("getPlayerData", UUID.class);
                var data = getData.invoke(repo, uuid);
                if (data != null) {
                    var getGuildId = data.getClass().getMethod("getGuildId");
                    var guildId = (UUID) getGuildId.invoke(data);
                    if (guildId != null) {
                        var getGuildManager = pluginClass.getMethod("getGuildManager");
                        var guildManager = getGuildManager.invoke(plugin);
                        var getGuild = guildManager.getClass().getMethod("getGuild", UUID.class);
                        var guild = getGuild.invoke(guildManager, guildId);
                        if (guild != null) {
                            var getName = guild.getClass().getMethod("getName");
                            return (String) getName.invoke(guild);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static int getOnlineCount() {
        try {
            return Universe.get().getPlayers().size();
        } catch (Exception ignored) {}
        return 0;
    }
}
