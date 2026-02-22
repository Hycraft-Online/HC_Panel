package com.hcpanel.gui;

import com.hcpanel.HC_PanelPlugin;
import com.hcpanel.gui.content.*;

// HC_Factions is a required dependency
import com.hcfactions.HC_FactionsPlugin;
// All other plugin deps are optional - detected at runtime via reflection

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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified Panel GUI - Single page that handles all navigation and content rendering.
 *
 * Navigation Levels:
 * - Root (null module): Shows Factions/Honor/Attributes in sidebar, CLOSE button
 * - Module level: Shows module's items in sidebar, BACK button
 * - Sub-views: Some views have nested navigation, BACK button
 */
public class UnifiedPanelGui extends InteractiveCustomUIPage<UnifiedPanelGui.PanelEventData> {

    private final HC_PanelPlugin plugin;

    // Navigation state - encoded as strings for simplicity
    // Examples: null (root), "honor" (module root), "honor:brackets" (specific view)
    private final String currentPath;
    private final String previousPath;

    // Content renderers (always available)
    private final NewsContentRenderer newsRenderer;
    private final GuideContentRenderer guideRenderer;
    private final FactionsContentRenderer factionsRenderer;
    // Content renderers (lazy - only created if their plugin is loaded)
    private HonorContentRenderer honorRenderer;
    private AttributesContentRenderer attributesRenderer;
    private ClassesContentRenderer classesRenderer;
    private CharacterContentRenderer characterRenderer;
    private SettingsContentRenderer settingsRenderer;
    private SkillsContentRenderer skillsRenderer;

    // Pending input values
    private String pendingGuildName;
    private String pendingGuildTag;
    private String pendingInvitePlayer;
    private String pendingBrowserSearch;

    // State passed between page instances
    private String browserSearch;

    /**
     * Create panel at root level (main menu)
     */
    public UnifiedPanelGui(@NonNullDecl HC_PanelPlugin plugin, @NonNullDecl PlayerRef playerRef) {
        this(plugin, playerRef, null, null);
    }

    /**
     * Create panel at specific navigation path
     */
    public UnifiedPanelGui(@NonNullDecl HC_PanelPlugin plugin, @NonNullDecl PlayerRef playerRef,
                          String currentPath, String previousPath) {
        super(playerRef, CustomPageLifetime.CanDismiss, PanelEventData.CODEC);
        this.plugin = plugin;
        this.currentPath = currentPath;
        this.previousPath = previousPath;

        // Initialize always-available renderers
        this.newsRenderer = new NewsContentRenderer(playerRef, plugin.getNewsConfig());
        this.guideRenderer = new GuideContentRenderer(playerRef);
        this.factionsRenderer = new FactionsContentRenderer(playerRef);
        // Initialize optional renderers only if their plugin is loaded
        if (isModuleAvailable("honor")) this.honorRenderer = new HonorContentRenderer(playerRef);
        if (isModuleAvailable("attributes")) this.attributesRenderer = new AttributesContentRenderer(playerRef);
        if (isModuleAvailable("classes")) this.classesRenderer = new ClassesContentRenderer(playerRef);
        if (isModuleAvailable("character")) this.characterRenderer = new CharacterContentRenderer(playerRef);
        this.settingsRenderer = new SettingsContentRenderer(playerRef);
        if (isModuleAvailable("skills")) this.skillsRenderer = new SkillsContentRenderer(playerRef);
    }

    public void setBrowserSearch(String search) {
        this.browserSearch = search;
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder cmd,
                     @NonNullDecl UIEventBuilder events, @NonNullDecl Store<EntityStore> store) {
        cmd.append("Pages/HC_Panel_Unified.ui");

        // Determine what to show based on current path
        if (currentPath == null) {
            buildRootMenu(cmd, events);
        } else {
            String[] parts = currentPath.split(":");
            String module = parts[0];
            String view = parts.length > 1 ? parts[1] : null;
            String subView = parts.length > 2 ? parts[2] : null;

            buildModuleView(cmd, events, store, module, view, subView);
        }
    }

    /**
     * Build the root menu showing available modules
     */
    private void buildRootMenu(UICommandBuilder cmd, UIEventBuilder events) {
        // Header
        cmd.set("#HeaderTitle.Text", "CHARACTER PANEL");
        cmd.set("#HeaderTitle.Style.TextColor", "#d4af37");
        cmd.set("#HeaderSubtitle.Text", "Welcome, " + playerRef.getUsername());

        // Sidebar buttons - News and Guide first, then plugin-dependent modules
        List<SidebarButton> buttons = new ArrayList<>();

        // Always-available modules
        buttons.add(new SidebarButton("News", "nav:news", null, "#4a9eff", true)); // News is default/active
        buttons.add(new SidebarButton("Guide", "nav:guide", null, "#4ecdc4"));

        // Plugin-dependent modules
        if (isModuleAvailable("factions")) {
            String badge = factionsRenderer.getInvitationBadge();
            buttons.add(new SidebarButton("Factions", "nav:factions:guild", badge, "#4a9eff"));
        }
        if (isModuleAvailable("honor")) {
            buttons.add(new SidebarButton("Honor", "nav:honor:standing", null, "#d4af37"));
        }
        if (isModuleAvailable("character")) {
            String badge = characterRenderer != null ? characterRenderer.getCombinedPointsBadge() : null;
            buttons.add(new SidebarButton("Character", "nav:character:overview", badge, "#d4af37"));
        }
        if (isModuleAvailable("skills")) {
            buttons.add(new SidebarButton("Skills", "nav:skills:professions", null, "#4ecdc4"));
        }

        // Always-available settings
        buttons.add(new SidebarButton("Settings", "nav:settings:combat", null, "#6c7a89"));

        renderSidebar(cmd, events, buttons);

        // Show CLOSE, hide BACK
        cmd.set("#BackButton.Visible", false);
        cmd.set("#CloseButton.Visible", true);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
            EventData.of("Action", "close"), false);

        // Show News content by default (instead of welcome)
        newsRenderer.renderContent(cmd, events);
    }

    /**
     * Build a module-specific view
     */
    private void buildModuleView(UICommandBuilder cmd, UIEventBuilder events, Store<EntityStore> store,
                                 String module, String view, String subView) {
        // Header based on module
        String headerColor = switch (module) {
            case "news" -> "#4a9eff";
            case "guide" -> "#4ecdc4";
            case "factions" -> "#4a9eff";
            case "honor" -> "#d4af37";
            case "attributes" -> "#8b5cf6";
            case "classes" -> "#8b4513";
            case "character" -> "#d4af37";
            case "skills" -> "#4ecdc4";
            case "settings" -> "#6c7a89";
            default -> "#ffffff";
        };

        String headerTitle = switch (module) {
            case "news" -> "NEWS";
            case "guide" -> "GUIDE";
            case "factions" -> "FACTIONS";
            case "honor" -> "HONOR";
            case "attributes" -> "ATTRIBUTES";
            case "classes" -> "CLASSES";
            case "character" -> "CHARACTER";
            case "skills" -> "SKILLS";
            case "settings" -> "SETTINGS";
            default -> "PANEL";
        };

        cmd.set("#HeaderTitle.Text", headerTitle);
        cmd.set("#HeaderTitle.Style.TextColor", headerColor);
        cmd.set("#HeaderSubtitle.Text", getPlayerSubtitle());

        // Build sidebar based on module
        List<SidebarButton> buttons = switch (module) {
            case "news" -> newsRenderer.getSidebarButtons(view);
            case "guide" -> guideRenderer.getSidebarButtons(view);
            case "honor" -> honorRenderer.getSidebarButtons(view);
            case "attributes" -> attributesRenderer.getSidebarButtons(view);
            case "factions" -> factionsRenderer.getSidebarButtons(view);
            case "classes" -> classesRenderer.getSidebarButtons(view);
            case "character" -> characterRenderer.getSidebarButtons(view);
            case "skills" -> skillsRenderer.getSidebarButtons(view);
            case "settings" -> settingsRenderer.getSidebarButtons(view);
            default -> new ArrayList<>();
        };

        renderSidebar(cmd, events, buttons);

        // Show BACK, hide CLOSE
        cmd.set("#BackButton.Visible", true);
        cmd.set("#CloseButton.Visible", false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton",
            EventData.of("Action", "back"), false);

        // Render content based on module and view
        switch (module) {
            case "news" -> newsRenderer.renderContent(cmd, events);
            case "guide" -> guideRenderer.renderContent(cmd, events, view);
            case "honor" -> honorRenderer.renderContent(cmd, events, view, subView);
            case "attributes" -> attributesRenderer.renderContent(cmd, events, store, playerRef, view);
            case "factions" -> {
                cmd.set("#HeaderSection.Visible", false);
                cmd.set("#HeaderBorder.Visible", false);
                cmd.set("#HeaderSpacer.Visible", false);
                if (browserSearch != null) factionsRenderer.setBrowserSearch(browserSearch);
                factionsRenderer.renderContent(cmd, events, store, playerRef, view, subView);
            }
            case "classes" -> classesRenderer.renderContent(cmd, events, store, playerRef, view);
            case "character" -> characterRenderer.renderContent(cmd, events, store, playerRef, view);
            case "skills" -> skillsRenderer.renderContent(cmd, events, view);
            case "settings" -> settingsRenderer.renderContent(cmd, events, view);
        }
    }

    /**
     * Render sidebar buttons
     */
    private void renderSidebar(UICommandBuilder cmd, UIEventBuilder events, List<SidebarButton> buttons) {
        for (int i = 0; i < 7; i++) {
            String btnId = "#SidebarBtn" + i;

            if (i < buttons.size()) {
                SidebarButton btn = buttons.get(i);
                cmd.set(btnId + ".Visible", true);

                // Include badge in button text if present
                String label = btn.label;
                if (btn.badge != null) {
                    label = btn.label + " (" + btn.badge + ")";
                }
                cmd.set(btnId + ".Text", label);

                // Highlight if active
                if (btn.active) {
                    cmd.set(btnId + ".Style.Default.Background", "#1b2532");
                    cmd.set(btnId + ".Style.Default.LabelStyle.TextColor", "#ffffff");
                }

                // Event binding
                events.addEventBinding(CustomUIEventBindingType.Activating, btnId,
                    EventData.of("Action", btn.action), false);
            } else {
                cmd.set(btnId + ".Visible", false);
            }
        }
    }

    /**
     * Render welcome content for root menu
     */
    private void renderWelcomeContent(UICommandBuilder cmd) {
        // Header
        cmd.set("#HeaderSubtitle.Text", "Welcome, " + playerRef.getUsername());
        cmd.set("#HeaderSubtitle.Style.TextColor", "#d4af37");
        cmd.set("#HeaderInfo.Text", "Select a module from the sidebar");

        // Content
        cmd.set("#ContentText.Visible", true);

        StringBuilder content = new StringBuilder();
        content.append("Select a module from the sidebar to view your character information.\n\n");

        if (isModuleAvailable("factions")) {
            content.append("FACTIONS - Manage your guild and faction\n");
        }
        if (isModuleAvailable("honor")) {
            content.append("HONOR - View PvP stats and rankings\n");
        }
        if (isModuleAvailable("attributes")) {
            content.append("ATTRIBUTES - View and allocate stats\n");
        }
        if (isModuleAvailable("classes")) {
            content.append("CLASSES - View and allocate talents\n");
        }

        cmd.set("#ContentText.Text", content.toString());

        // Footer
        cmd.set("#FooterText.Text", "Press ESC to close this panel.");
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store,
                               @NonNullDecl PanelEventData data) {
        super.handleDataEvent(ref, store, data);

        // Track text field inputs
        if (data.guildNameInput != null) {
            this.pendingGuildName = data.guildNameInput;
        }
        if (data.guildTagInput != null) {
            this.pendingGuildTag = data.guildTagInput;
        }
        if (data.invitePlayerInput != null) {
            this.pendingInvitePlayer = data.invitePlayerInput;
        }
        if (data.browserSearchInput != null) {
            this.pendingBrowserSearch = data.browserSearchInput;
        }

        if (data.action == null) return;

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        // Handle close
        if ("close".equals(data.action)) {
            this.close();
            return;
        }

        // Handle back navigation
        if ("back".equals(data.action)) {
            String backPath = calculateBackPath();
            player.getPageManager().openCustomPage(ref, store,
                new UnifiedPanelGui(plugin, playerRef, backPath, null));
            return;
        }

        // Handle navigation (nav:module or nav:module:view)
        if (data.action.startsWith("nav:")) {
            String newPath = data.action.substring(4);
            player.getPageManager().openCustomPage(ref, store,
                new UnifiedPanelGui(plugin, playerRef, newPath, currentPath));
            return;
        }

        // Handle actions (action:type:params)
        if (data.action.startsWith("action:")) {
            handleAction(ref, store, player, data.action.substring(7));
        }
    }

    /**
     * Calculate the path for back navigation.
     * Always returns to main menu from any module view.
     */
    private String calculateBackPath() {
        // Always go back to main menu
        return null;
    }

    /**
     * Handle action events (purchases, allocations, etc.)
     */
    private void handleAction(Ref<EntityStore> ref, Store<EntityStore> store, Player player, String actionData) {
        String[] parts = actionData.split(":");
        if (parts.length < 2) return;

        String actionType = parts[0];
        // Rejoin remaining parts for actions with multiple colons
        String actionParam = actionData.substring(actionType.length() + 1);

        switch (actionType) {
            case "buy_honor" -> handleHonorPurchase(ref, store, player, actionParam);
            case "allocate" -> handleStatAllocation(ref, store, player, actionParam);
            case "guild_action", "guild_join", "invite_accept", "invite_decline",
                 "leave_guild", "create_guild", "promote", "demote", "kick",
                 "set_tag", "clear_tag", "invite_player" ->
                handleGuildAction(ref, store, player, actionType + ":" + actionParam);
            case "browser_search" -> handleBrowserSearch(ref, store, player);
            case "talent" -> handleTalentAction(ref, store, player, actionParam);
            case "setting" -> handleSettingToggle(ref, store, player, actionParam);
        }
    }

    private void handleHonorPurchase(Ref<EntityStore> ref, Store<EntityStore> store, Player player, String itemIndex) {
        boolean success = honorRenderer.handlePurchase(playerRef, itemIndex);
        if (success) {
            // Refresh the page
            player.getPageManager().openCustomPage(ref, store,
                new UnifiedPanelGui(plugin, playerRef, currentPath, previousPath));
        }
    }

    private void handleStatAllocation(Ref<EntityStore> ref, Store<EntityStore> store, Player player, String attribute) {
        boolean success = attributesRenderer.handleAllocation(playerRef, attribute);
        if (success) {
            // Refresh the page
            player.getPageManager().openCustomPage(ref, store,
                new UnifiedPanelGui(plugin, playerRef, currentPath, previousPath));
        }
    }

    private void handleGuildAction(Ref<EntityStore> ref, Store<EntityStore> store, Player player, String action) {
        // Pass pending text inputs for various actions
        factionsRenderer.handleGuildAction(playerRef, action, pendingGuildName, pendingGuildTag, pendingInvitePlayer);
        // Refresh the page
        player.getPageManager().openCustomPage(ref, store,
            new UnifiedPanelGui(plugin, playerRef, currentPath, previousPath));
    }

    private void handleBrowserSearch(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        UnifiedPanelGui newPage = new UnifiedPanelGui(plugin, playerRef, currentPath, previousPath);
        newPage.setBrowserSearch(pendingBrowserSearch);
        player.getPageManager().openCustomPage(ref, store, newPage);
    }

    private void handleTalentAction(Ref<EntityStore> ref, Store<EntityStore> store, Player player, String actionData) {
        String[] parts = actionData.split(":");
        if (parts.length < 2) return;

        String subAction = parts[0];
        String param = parts[1];

        switch (subAction) {
            case "allocate" -> {
                String error = classesRenderer.handleTalentAllocation(playerRef, param);
                if (error != null) {
                    playerRef.sendMessage(com.hypixel.hytale.server.core.Message.raw(error).color("red"));
                }
            }
            case "reset" -> {
                int refunded = classesRenderer.handleTalentReset(playerRef);
                if (refunded > 0) {
                    playerRef.sendMessage(com.hypixel.hytale.server.core.Message.raw("Refunded " + refunded + " talent points.").color("green"));
                }
            }
        }

        // Re-render talent tree in-place (preserves scroll position)
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        classesRenderer.renderContent(cmd, events, store, playerRef, "talents");
        sendUpdate(cmd, events, false);
    }

    private void handleSettingToggle(Ref<EntityStore> ref, Store<EntityStore> store, Player player, String setting) {
        settingsRenderer.handleToggle(playerRef, setting, store.getExternalData().getWorld());
        // Refresh the page
        player.getPageManager().openCustomPage(ref, store,
            new UnifiedPanelGui(plugin, playerRef, currentPath, previousPath));
    }

    private boolean isModuleAvailable(String module) {
        return switch (module) {
            case "factions" -> HC_FactionsPlugin.getInstance() != null;
            case "honor" -> isPluginLoaded("com.hchonor.HC_HonorPlugin");
            case "attributes" -> isPluginLoaded("com.hcattributes.HC_AttributesPlugin");
            case "classes" -> isPluginLoaded("com.hcclasses.HC_ClassesPlugin");
            case "character" -> isPluginLoaded("com.hcattributes.HC_AttributesPlugin") || isPluginLoaded("com.hcclasses.HC_ClassesPlugin");
            case "skills" -> isPluginLoaded("com.hcprofessions.HC_ProfessionsPlugin");
            default -> false;
        };
    }

    private static boolean isPluginLoaded(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Object instance = clazz.getMethod("getInstance").invoke(null);
            return instance != null;
        } catch (Exception e) {
            return false;
        }
    }

    private String getPlayerSubtitle() {
        String factionTag = factionsRenderer.getFactionTag();
        if (factionTag != null) {
            return "[" + factionTag + "] " + playerRef.getUsername();
        }
        return playerRef.getUsername();
    }

    /**
     * Sidebar button data - public for use by content renderers
     */
    public record SidebarButton(String label, String action, String badge, String color, boolean active) {
        public SidebarButton(String label, String action, String badge, String color) {
            this(label, action, badge, color, false);
        }

        public SidebarButton withActive(boolean active) {
            return new SidebarButton(label, action, badge, color, active);
        }
    }

    /**
     * Event data codec
     */
    public static class PanelEventData {
        public static final BuilderCodec<PanelEventData> CODEC = BuilderCodec.<PanelEventData>builder(
                PanelEventData.class, PanelEventData::new)
            .addField(new KeyedCodec<>("Action", Codec.STRING),
                (d, s) -> d.action = s, d -> d.action)
            .addField(new KeyedCodec<>("@GuildNameInput", Codec.STRING),
                (d, s) -> d.guildNameInput = s, d -> d.guildNameInput)
            .addField(new KeyedCodec<>("@GuildTagInput", Codec.STRING),
                (d, s) -> d.guildTagInput = s, d -> d.guildTagInput)
            .addField(new KeyedCodec<>("@InvitePlayerInput", Codec.STRING),
                (d, s) -> d.invitePlayerInput = s, d -> d.invitePlayerInput)
            .addField(new KeyedCodec<>("@BrowserSearchInput", Codec.STRING),
                (d, s) -> d.browserSearchInput = s, d -> d.browserSearchInput)
            .build();

        private String action;
        private String guildNameInput;
        private String guildTagInput;
        private String invitePlayerInput;
        private String browserSearchInput;
    }
}
