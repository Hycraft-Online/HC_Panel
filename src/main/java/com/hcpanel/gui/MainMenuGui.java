package com.hcpanel.gui;

import com.hcpanel.HC_PanelPlugin;
import com.hcpanel.gui.modules.ModuleContentProvider;
import com.hcpanel.gui.modules.ModuleContentProvider.SidebarItem;

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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.List;

/**
 * Main menu GUI for the unified HC_Panel.
 * Displays available modules in the sidebar and handles navigation.
 */
public class MainMenuGui extends InteractiveCustomUIPage<MainMenuGui.MenuEventData> {

    private final HC_PanelPlugin plugin;
    private final List<ModuleContentProvider> modules;
    private final NavigationContext context;

    // Current state
    private ModuleContentProvider activeModule = null;
    private String activeView = null;

    public MainMenuGui(@NonNullDecl HC_PanelPlugin plugin, @NonNullDecl PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, MenuEventData.CODEC);
        this.plugin = plugin;
        this.modules = plugin.getAvailableModules();
        this.context = new NavigationContext(); // Root context
    }

    /**
     * Constructor for module view - shows a specific module's content.
     */
    public MainMenuGui(@NonNullDecl HC_PanelPlugin plugin, @NonNullDecl PlayerRef playerRef,
                      @NonNullDecl ModuleContentProvider module, String view) {
        super(playerRef, CustomPageLifetime.CanDismiss, MenuEventData.CODEC);
        this.plugin = plugin;
        this.modules = plugin.getAvailableModules();
        this.activeModule = module;
        this.activeView = view;
        this.context = new NavigationContext(module, view, new NavigationContext());
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder cmd,
                     @NonNullDecl UIEventBuilder events, @NonNullDecl Store<EntityStore> store) {

        if (activeModule != null) {
            // Module view mode - show module-specific sidebar and content
            buildModuleView(cmd, events);
        } else {
            // Main menu mode - show module list
            buildMainMenu(cmd, events);
        }
    }

    private void buildMainMenu(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.append("Pages/HC_Panel_MainMenu.ui");

        // Set header
        cmd.set("#HeaderTitle.Text", "CHARACTER PANEL");
        cmd.set("#HeaderSubtitle.Text", "Welcome, " + playerRef.getUsername());

        // Hide module-specific content, show welcome content
        cmd.set("#ModuleContent.Visible", false);
        cmd.set("#WelcomeContent.Visible", true);

        // Build module buttons in sidebar
        int buttonIndex = 0;
        for (ModuleContentProvider module : modules) {
            String buttonId = "#ModuleButton" + buttonIndex;

            cmd.set(buttonId + ".Visible", true);
            cmd.set(buttonId + ".Text", module.getModuleName());

            // Set badge if present
            String badge = module.getBadgeText(playerRef);
            String badgeId = "#ModuleBadge" + buttonIndex;
            if (badge != null) {
                cmd.set(badgeId + ".Visible", true);
                cmd.set(badgeId + "Text.Text", badge);
            } else {
                cmd.set(badgeId + ".Visible", false);
            }

            // Bind click event - encode module ID in action string
            events.addEventBinding(CustomUIEventBindingType.Activating, buttonId,
                EventData.of("Action", "OpenModule:" + module.getModuleId()), false);

            buttonIndex++;
        }

        // Hide unused buttons (max 5 module slots)
        for (int i = buttonIndex; i < 5; i++) {
            cmd.set("#ModuleButton" + i + ".Visible", false);
            cmd.set("#ModuleBadge" + i + ".Visible", false);
        }

        // Show CLOSE button
        cmd.set("#BackButton.Visible", false);
        cmd.set("#CloseButton.Visible", true);

        // Bind close button
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
            EventData.of("Action", "Close"), false);

        // Set welcome content based on available modules
        StringBuilder welcomeText = new StringBuilder();
        welcomeText.append("Use the sidebar to navigate between modules.\n\n");
        welcomeText.append("Available modules:\n");
        for (ModuleContentProvider module : modules) {
            welcomeText.append("  - ").append(module.getModuleName()).append("\n");
        }
        if (modules.isEmpty()) {
            welcomeText.append("No modules available - install HC_Factions, HC_Honor, or HC_Attributes.");
        }
        cmd.set("#WelcomeText.Text", welcomeText.toString());
    }

    private void buildModuleView(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.append("Pages/HC_Panel_MainMenu.ui");

        // Set header with module name
        cmd.set("#HeaderTitle.Text", activeModule.getModuleName().toUpperCase());
        cmd.set("#HeaderTitle.Style.TextColor", activeModule.getHeaderColor());

        // Get player info for subtitle
        String subtitle = getPlayerSubtitle();
        cmd.set("#HeaderSubtitle.Text", subtitle);

        // Build sidebar with module's items
        List<SidebarItem> sidebarItems = activeModule.getSidebarItems(playerRef);

        int buttonIndex = 0;
        for (SidebarItem item : sidebarItems) {
            // Skip officer-only items if not officer
            if (item.officerOnly() && !isPlayerOfficer()) {
                continue;
            }

            String buttonId = "#ModuleButton" + buttonIndex;

            cmd.set(buttonId + ".Visible", true);
            cmd.set(buttonId + ".Text", item.displayName());

            // Highlight active view
            if (item.viewId().equals(activeView)) {
                cmd.set(buttonId + ".Style.Default.Background", "#1b2532");
                cmd.set(buttonId + ".Style.Default.LabelStyle.TextColor", "#ffffff");
            }

            // Set badge if present
            String badgeId = "#ModuleBadge" + buttonIndex;
            if (item.badgeText() != null) {
                cmd.set(badgeId + ".Visible", true);
                cmd.set(badgeId + "Text.Text", item.badgeText());
            } else {
                cmd.set(badgeId + ".Visible", false);
            }

            // Bind click event - encode view ID in action string
            events.addEventBinding(CustomUIEventBindingType.Activating, buttonId,
                EventData.of("Action", "OpenView:" + item.viewId()), false);

            buttonIndex++;
        }

        // Hide unused buttons
        for (int i = buttonIndex; i < 5; i++) {
            cmd.set("#ModuleButton" + i + ".Visible", false);
            cmd.set("#ModuleBadge" + i + ".Visible", false);
        }

        // Show BACK button, hide CLOSE
        cmd.set("#BackButton.Visible", true);
        cmd.set("#CloseButton.Visible", false);

        // Bind back button
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton",
            EventData.of("Action", "Back"), false);

        // Hide welcome content, show module placeholder
        cmd.set("#WelcomeContent.Visible", false);
        cmd.set("#ModuleContent.Visible", true);
        cmd.set("#ModuleContentText.Text", "Select an option from the sidebar to view " +
            activeModule.getModuleName().toLowerCase() + " details.");
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store,
                               @NonNullDecl MenuEventData data) {
        super.handleDataEvent(ref, store, data);

        if (data.action == null) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        // Parse action string
        if (data.action.equals("Close")) {
            this.close();
            return;
        }

        if (data.action.equals("Back")) {
            // Return to main menu
            player.getPageManager().openCustomPage(ref, store,
                new MainMenuGui(plugin, playerRef));
            return;
        }

        if (data.action.startsWith("OpenModule:")) {
            String moduleId = data.action.substring("OpenModule:".length());

            // Find the module
            ModuleContentProvider targetModule = modules.stream()
                .filter(m -> m.getModuleId().equals(moduleId))
                .findFirst()
                .orElse(null);

            if (targetModule != null) {
                // Open module view with default view
                String defaultView = targetModule.getDefaultView();

                // Try to create and open the content page directly
                InteractiveCustomUIPage<?> contentPage = targetModule.createContentPage(
                    playerRef, defaultView, this);

                if (contentPage != null) {
                    player.getPageManager().openCustomPage(ref, store, contentPage);
                } else {
                    // If content page creation failed, show module view
                    player.getPageManager().openCustomPage(ref, store,
                        new MainMenuGui(plugin, playerRef, targetModule, defaultView));
                }
            }
            return;
        }

        if (data.action.startsWith("OpenView:")) {
            String viewId = data.action.substring("OpenView:".length());
            if (activeModule == null) return;

            // Create and open the content page
            InteractiveCustomUIPage<?> contentPage = activeModule.createContentPage(
                playerRef, viewId, this);

            if (contentPage != null) {
                player.getPageManager().openCustomPage(ref, store, contentPage);
            } else {
                // Special handling for land claims (needs position context)
                if ("land_claims".equals(viewId)) {
                    openLandClaimsWithPosition(ref, store, player);
                } else {
                    playerRef.sendMessage(Message.raw("Unable to open " + viewId)
                        .color(new Color(255, 100, 100)));
                }
            }
        }
    }

    /**
     * Special handler for land claims which needs player position.
     */
    private void openLandClaimsWithPosition(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        try {
            // Get player position
            var transformType = Class.forName("com.hypixel.hytale.server.core.modules.entity.component.TransformComponent");
            var getComponentType = transformType.getMethod("getComponentType");
            var componentType = getComponentType.invoke(null);

            var transform = store.getComponent(ref, (com.hypixel.hytale.component.ComponentType) componentType);
            if (transform == null) return;

            var getPosition = transform.getClass().getMethod("getPosition");
            var position = getPosition.invoke(transform);
            var getX = position.getClass().getMethod("getX");
            var getZ = position.getClass().getMethod("getZ");

            double x = (double) getX.invoke(position);
            double z = (double) getZ.invoke(position);
            String worldName = store.getExternalData().getWorld().getName();

            // Get faction and guild info
            Class<?> factionsPluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
            var getInstance = factionsPluginClass.getMethod("getInstance");
            var factionsPlugin = getInstance.invoke(null);

            var getRepo = factionsPluginClass.getMethod("getPlayerDataRepository");
            var repo = getRepo.invoke(factionsPlugin);
            var getData = repo.getClass().getMethod("getPlayerData", java.util.UUID.class);
            var playerData = getData.invoke(repo, playerRef.getUuid());

            if (playerData == null) return;

            var getGuildId = playerData.getClass().getMethod("getGuildId");
            var guildId = (java.util.UUID) getGuildId.invoke(playerData);

            var getFactionId = playerData.getClass().getMethod("getFactionId");
            var factionId = (String) getFactionId.invoke(playerData);

            if (guildId == null || factionId == null) return;

            // Calculate chunk coordinates
            var claimManagerClass = Class.forName("com.hcfactions.managers.ClaimManager");
            var toChunkCoord = claimManagerClass.getMethod("toChunkCoord", int.class);
            int chunkX = (int) toChunkCoord.invoke(null, (int) x);
            int chunkZ = (int) toChunkCoord.invoke(null, (int) z);

            // Create GuildClaimGui
            Class<?> guiClass = Class.forName("com.hcfactions.gui.GuildClaimGui");
            var constructor = guiClass.getConstructor(
                factionsPlugin.getClass(), PlayerRef.class,
                java.util.UUID.class, String.class, String.class, int.class, int.class
            );
            var gui = constructor.newInstance(factionsPlugin, playerRef, guildId, factionId, worldName, chunkX, chunkZ);

            player.getPageManager().openCustomPage(ref, store, (InteractiveCustomUIPage<?>) gui);

        } catch (Exception e) {
            playerRef.sendMessage(Message.raw("Unable to open land claims: " + e.getMessage())
                .color(new Color(255, 100, 100)));
        }
    }

    private String getPlayerSubtitle() {
        // Try to get faction tag and class
        String factionTag = getFactionTag();
        String className = getClassName();

        if (factionTag != null && className != null) {
            return "[" + factionTag + "] " + className;
        } else if (factionTag != null) {
            return "[" + factionTag + "] " + playerRef.getUsername();
        } else if (className != null) {
            return className;
        }
        return playerRef.getUsername();
    }

    private String getFactionTag() {
        try {
            Class<?> pluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
            var getInstance = pluginClass.getMethod("getInstance");
            var plugin = getInstance.invoke(null);
            if (plugin != null) {
                var getRepo = pluginClass.getMethod("getPlayerDataRepository");
                var repo = getRepo.invoke(plugin);
                var getData = repo.getClass().getMethod("getPlayerData", java.util.UUID.class);
                var data = getData.invoke(repo, playerRef.getUuid());
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
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private String getClassName() {
        try {
            Class<?> apiClass = Class.forName("com.hcclasses.api.HC_ClassesAPI");
            var getInstance = apiClass.getMethod("getInstance");
            var api = getInstance.invoke(null);
            if (api != null) {
                var getClassName = apiClass.getMethod("getPlayerClassName", java.util.UUID.class);
                return (String) getClassName.invoke(api, playerRef.getUuid());
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private boolean isPlayerOfficer() {
        try {
            Class<?> pluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
            var getInstance = pluginClass.getMethod("getInstance");
            var plugin = getInstance.invoke(null);
            if (plugin != null) {
                var getRepo = pluginClass.getMethod("getPlayerDataRepository");
                var repo = getRepo.invoke(plugin);
                var getData = repo.getClass().getMethod("getPlayerData", java.util.UUID.class);
                var data = getData.invoke(repo, playerRef.getUuid());
                if (data != null) {
                    var getRole = data.getClass().getMethod("getGuildRole");
                    var role = getRole.invoke(data);
                    if (role != null) {
                        var hasAtLeast = role.getClass().getMethod("hasAtLeast",
                            Class.forName("com.hcfactions.models.GuildRole"));
                        var officerRole = Class.forName("com.hcfactions.models.GuildRole")
                            .getField("OFFICER").get(null);
                        return (boolean) hasAtLeast.invoke(role, officerRole);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    /**
     * Data class for UI events
     */
    public static class MenuEventData {
        public static final BuilderCodec<MenuEventData> CODEC = BuilderCodec.<MenuEventData>builder(
                MenuEventData.class, MenuEventData::new)
            .addField(new KeyedCodec<>("Action", Codec.STRING),
                (d, s) -> d.action = s, d -> d.action)
            .build();

        private String action;
    }
}
