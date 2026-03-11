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
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private RecruitmentContentRenderer recruitmentRenderer;

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
        if (isModuleAvailable("recruitment")) this.recruitmentRenderer = new RecruitmentContentRenderer(playerRef);
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
        cmd.set("#HeaderTitle.TextSpans", Message.raw("CHARACTER PANEL").color(Color.decode("#d4af37")));
        int onlinePlayers = Universe.get().getPlayers().size();
        cmd.set("#HeaderSubtitle.Text", "Welcome, " + playerRef.getUsername() + "  |  " + onlinePlayers + " online");

        // Player info summary — use colored TextSpans for visual distinction
        UUID uuid = playerRef.getUuid();
        Message infoMsg = Message.raw("");
        boolean firstPart = true;

        String level = getPlayerLevel(uuid);
        if (level != null) {
            infoMsg.insert(Message.raw("Lv. " + level).color(Color.decode("#ffd700")));
            firstPart = false;
        }
        String className = getClassName(uuid);
        if (className != null) {
            if (!firstPart) infoMsg.insert(Message.raw("  |  ").color(Color.decode("#555555")));
            infoMsg.insert(Message.raw(className).color(Color.decode("#ff6b6b")));
            firstPart = false;
        }
        String factionName = getFactionName(uuid);
        if (factionName != null) {
            if (!firstPart) infoMsg.insert(Message.raw("  |  ").color(Color.decode("#555555")));
            infoMsg.insert(Message.raw(factionName).color(Color.decode("#4a9eff")));
            firstPart = false;
        }
        String guildName = getGuildName(uuid);
        if (guildName != null) {
            if (!firstPart) infoMsg.insert(Message.raw("  |  ").color(Color.decode("#555555")));
            infoMsg.insert(Message.raw("[" + guildName + "]").color(Color.decode("#4ecdc4")));
            firstPart = false;
        }
        String honorRankTitle = getHonorRankTitle(uuid);
        if (honorRankTitle != null) {
            if (!firstPart) infoMsg.insert(Message.raw("  |  ").color(Color.decode("#555555")));
            infoMsg.insert(Message.raw(honorRankTitle).color(Color.decode("#d4af37")));
            firstPart = false;
        }
        String professionName = getMainProfession(uuid);
        if (professionName != null) {
            if (!firstPart) infoMsg.insert(Message.raw("  |  ").color(Color.decode("#555555")));
            infoMsg.insert(Message.raw(professionName).color(Color.decode("#4ecdc4")));
            firstPart = false;
        }
        if (!firstPart) {
            cmd.set("#HeaderInfo.TextSpans", infoMsg);
        }

        // Unspent points badge (stat + talent points)
        if (characterRenderer != null) {
            String badge = characterRenderer.getCombinedPointsBadge();
            if (badge != null) {
                cmd.set("#HeaderBadge.Visible", true);
                cmd.set("#HeaderBadgeText.Text", badge + " unspent");
            }
        }

        // XP Progress bar
        try {
            Class<?> levelPluginClass = Class.forName("com.hcleveling.HC_LevelingPlugin");
            Object levelPlugin = levelPluginClass.getMethod("getInstance").invoke(null);
            if (levelPlugin != null) {
                Object levelManager = levelPluginClass.getMethod("getLevelManager").invoke(levelPlugin);
                if (levelManager != null) {
                    Object playerData = levelManager.getClass()
                        .getMethod("getPlayerData", UUID.class, String.class)
                        .invoke(levelManager, uuid, playerRef.getUsername());
                    if (playerData != null) {
                        int playerLevel = (int) playerData.getClass().getMethod("getLevel").invoke(playerData);
                        long currentXp = (long) playerData.getClass().getMethod("getCurrentXp").invoke(playerData);
                        int maxLevel = (int) levelManager.getClass().getMethod("getMaxLevel").invoke(levelManager);
                        boolean isMaxLevel = playerLevel >= maxLevel;

                        cmd.set("#ProgressRow.Visible", true);

                        if (isMaxLevel) {
                            cmd.set("#ProgressLabel.Text", "Level " + playerLevel + " (MAX)");
                            cmd.set("#ProgressPercent.Text", "100%");
                            Anchor barAnchor = new Anchor();
                            barAnchor.setWidth(Value.of(460));
                            barAnchor.setHeight(Value.of(18));
                            cmd.setObject("#ProgressBar.Anchor", barAnchor);
                            cmd.set("#ProgressBar.Background", "#d4af37");
                        } else {
                            long xpForCurrent = (long) levelManager.getClass()
                                .getMethod("getXpForLevel", int.class).invoke(levelManager, playerLevel);
                            long xpForNext = (long) levelManager.getClass()
                                .getMethod("getXpForLevel", int.class).invoke(levelManager, playerLevel + 1);
                            long xpInLevel = currentXp - xpForCurrent;
                            long xpNeeded = xpForNext - xpForCurrent;
                            float progress = xpNeeded > 0 ? (float) xpInLevel / xpNeeded : 0;
                            progress = Math.max(0, Math.min(1, progress));
                            int percent = (int) (progress * 100);
                            int barWidth = Math.max(1, (int) (460 * progress));

                            cmd.set("#ProgressLabel.Text", "Level " + playerLevel + " \u2192 " + (playerLevel + 1));
                            cmd.set("#ProgressPercent.Text", percent + "%");
                            Anchor barAnchor = new Anchor();
                            barAnchor.setWidth(Value.of(barWidth));
                            barAnchor.setHeight(Value.of(18));
                            cmd.setObject("#ProgressBar.Anchor", barAnchor);
                            cmd.set("#ProgressBar.Background", "#4ecdc4");
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // Sidebar buttons - News and Guide first, then plugin-dependent modules
        List<SidebarButton> buttons = new ArrayList<>();

        // Always-available modules
        buttons.add(new SidebarButton("News", "nav:news", null, "#4a9eff", true)); // News is default/active
        buttons.add(new SidebarButton("Guide", "nav:guide", null, "#4ecdc4"));

        // Plugin-dependent modules
        if (isModuleAvailable("factions")) {
            String badge = factionsRenderer.getInvitationBadge();
            buttons.add(new SidebarButton("Guild", "nav:factions:guild", badge, "#4a9eff"));
        }
        if (isModuleAvailable("honor")) {
            String honorBadge = getHonorRankBadge(uuid);
            buttons.add(new SidebarButton("Honor", "nav:honor:standing", honorBadge, "#d4af37"));
        }
        if (isModuleAvailable("character")) {
            String badge = characterRenderer != null ? characterRenderer.getCombinedPointsBadge() : null;
            buttons.add(new SidebarButton("Character", "nav:character:overview", badge, "#d4af37"));
        }
        if (isModuleAvailable("skills")) {
            buttons.add(new SidebarButton("Skills", "nav:skills:professions", null, "#4ecdc4"));
        }
        if (isModuleAvailable("recruitment")) {
            String recruitBadge = recruitmentRenderer != null ? recruitmentRenderer.getBidBadge() : null;
            buttons.add(new SidebarButton("Recruitment", "nav:recruitment:rank", recruitBadge, "#ffd700"));
        }
        if (isModuleAvailable("characters")) {
            String charBadge = getCharacterCount(uuid);
            buttons.add(new SidebarButton("Characters", "open:characters", charBadge, "#22d3ee"));
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
            case "recruitment" -> "#ffd700";
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
            case "recruitment" -> "RECRUITMENT";
            case "settings" -> "SETTINGS";
            default -> "PANEL";
        };

        cmd.set("#HeaderTitle.TextSpans", Message.raw(headerTitle).color(Color.decode(headerColor)));
        cmd.set("#HeaderSubtitle.Text", getPlayerSubtitle());

        // Build sidebar based on module (null-safe for optional renderers)
        List<SidebarButton> buttons = switch (module) {
            case "news" -> newsRenderer.getSidebarButtons(view);
            case "guide" -> guideRenderer.getSidebarButtons(view);
            case "honor" -> honorRenderer != null ? honorRenderer.getSidebarButtons(view) : new ArrayList<>();
            case "attributes" -> attributesRenderer != null ? attributesRenderer.getSidebarButtons(view) : new ArrayList<>();
            case "factions" -> factionsRenderer.getSidebarButtons(view);
            case "classes" -> classesRenderer != null ? classesRenderer.getSidebarButtons(view) : new ArrayList<>();
            case "character" -> characterRenderer != null ? characterRenderer.getSidebarButtons(view) : new ArrayList<>();
            case "skills" -> skillsRenderer != null ? skillsRenderer.getSidebarButtons(view) : new ArrayList<>();
            case "recruitment" -> recruitmentRenderer != null ? recruitmentRenderer.getSidebarButtons(view) : new ArrayList<>();
            case "settings" -> settingsRenderer.getSidebarButtons(view);
            default -> new ArrayList<>();
        };

        renderSidebar(cmd, events, buttons);

        // Show BACK, hide CLOSE
        cmd.set("#BackButton.Visible", true);
        cmd.set("#CloseButton.Visible", false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BackButton",
            EventData.of("Action", "back"), false);

        // Render content based on module and view (null-safe for optional renderers)
        switch (module) {
            case "news" -> newsRenderer.renderContent(cmd, events);
            case "guide" -> guideRenderer.renderContent(cmd, events, view);
            case "honor" -> {
                if (honorRenderer != null) honorRenderer.renderContent(cmd, events, view, subView);
                else showModuleUnavailable(cmd, "Honor");
            }
            case "attributes" -> {
                if (attributesRenderer != null) attributesRenderer.renderContent(cmd, events, store, playerRef, view);
                else showModuleUnavailable(cmd, "Attributes");
            }
            case "factions" -> {
                cmd.set("#HeaderSection.Visible", false);
                cmd.set("#HeaderBorder.Visible", false);
                cmd.set("#HeaderSpacer.Visible", false);
                if (browserSearch != null) factionsRenderer.setBrowserSearch(browserSearch);
                factionsRenderer.renderContent(cmd, events, store, playerRef, view, subView);
            }
            case "classes" -> {
                if (classesRenderer != null) classesRenderer.renderContent(cmd, events, store, playerRef, view);
                else showModuleUnavailable(cmd, "Classes");
            }
            case "character" -> {
                if (characterRenderer != null) characterRenderer.renderContent(cmd, events, store, playerRef, view);
                else showModuleUnavailable(cmd, "Character");
            }
            case "skills" -> {
                if (skillsRenderer != null) skillsRenderer.renderContent(cmd, events, view);
                else showModuleUnavailable(cmd, "Skills");
            }
            case "recruitment" -> {
                if (recruitmentRenderer != null) recruitmentRenderer.renderContent(cmd, events, view);
                else showModuleUnavailable(cmd, "Recruitment");
            }
            case "settings" -> settingsRenderer.renderContent(cmd, events, view);
        }
    }

    private static final int MAX_SIDEBAR_BUTTONS = 10;

    /**
     * Render sidebar buttons with accent color indicators
     */
    private void renderSidebar(UICommandBuilder cmd, UIEventBuilder events, List<SidebarButton> buttons) {
        for (int i = 0; i < MAX_SIDEBAR_BUTTONS; i++) {
            String rowId = "#SidebarBtn" + i + "Row";
            String btnId = "#SidebarBtn" + i;
            String accentId = "#SidebarBtn" + i + "Accent";

            if (i < buttons.size()) {
                SidebarButton btn = buttons.get(i);
                cmd.set(rowId + ".Visible", true);

                // Include badge in button text if present
                String label = btn.label;
                if (btn.badge != null) {
                    label = btn.label + " (" + btn.badge + ")";
                }
                cmd.set(btnId + ".Text", label);

                // Highlight active button with accent color bar and white text
                if (btn.active) {
                    cmd.set(btnId + ".Style.Default.Background", "#1b2532");
                    cmd.set(btnId + ".Style.Default.LabelStyle.TextColor", "#ffffff");
                    cmd.set(accentId + ".Background", btn.color != null ? btn.color : "#ffffff");
                }

                // Event binding
                events.addEventBinding(CustomUIEventBindingType.Activating, btnId,
                    EventData.of("Action", btn.action), false);
            } else {
                cmd.set(rowId + ".Visible", false);
            }
        }
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

        // Handle opening character select (HC_MultiChar)
        if ("open:characters".equals(data.action)) {
            openCharacterSelect(ref, store, player);
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
     * Navigates up one level: subview -> module root -> main menu.
     */
    private String calculateBackPath() {
        if (currentPath == null) return null;

        // If path has sub-levels (e.g. "guide:combat"), go up to module root ("guide")
        int lastColon = currentPath.lastIndexOf(':');
        if (lastColon > 0) {
            String parentPath = currentPath.substring(0, lastColon);
            // If parent is just the module name (no more colons), go to main menu
            // since the module root view is the default when entering from main menu
            if (!parentPath.contains(":")) {
                return null;
            }
            return parentPath;
        }

        // Module root (e.g. "guide") -> main menu
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
                 "set_tag", "clear_tag", "invite_player", "disband_guild" ->
                handleGuildAction(ref, store, player, actionType + ":" + actionParam);
            case "browser_search" -> handleBrowserSearch(ref, store, player);
            case "talent" -> handleTalentAction(ref, store, player, actionParam);
            case "setting" -> handleSettingToggle(ref, store, player, actionParam);
            case "recruit_accept", "recruit_decline", "recruit_withdraw", "recruit_bid" ->
                handleRecruitmentAction(ref, store, player, actionType, actionParam);
        }
    }

    private void handleHonorPurchase(Ref<EntityStore> ref, Store<EntityStore> store, Player player, String itemIndex) {
        if (honorRenderer == null) return;
        boolean success = honorRenderer.handlePurchase(playerRef, itemIndex);
        if (success) {
            refreshCurrentView(store);
        }
    }

    private void handleStatAllocation(Ref<EntityStore> ref, Store<EntityStore> store, Player player, String attribute) {
        if (attributesRenderer == null) return;
        boolean success = attributesRenderer.handleAllocation(playerRef, attribute);
        if (success) {
            refreshCurrentView(store);
        }
    }

    private void handleGuildAction(Ref<EntityStore> ref, Store<EntityStore> store, Player player, String action) {
        factionsRenderer.handleGuildAction(playerRef, action, pendingGuildName, pendingGuildTag, pendingInvitePlayer);
        refreshCurrentView(store);
    }

    private void handleBrowserSearch(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        this.browserSearch = pendingBrowserSearch;
        refreshCurrentView(store);
    }

    private void handleTalentAction(Ref<EntityStore> ref, Store<EntityStore> store, Player player, String actionData) {
        if (classesRenderer == null) return;
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
        refreshCurrentView(store);
    }

    private void handleRecruitmentAction(Ref<EntityStore> ref, Store<EntityStore> store, Player player,
                                          String actionType, String actionParam) {
        if (recruitmentRenderer == null) return;

        String error = switch (actionType) {
            case "recruit_accept" -> recruitmentRenderer.handleAcceptBid(actionParam);
            case "recruit_decline" -> recruitmentRenderer.handleDeclineBid(actionParam);
            case "recruit_withdraw" -> {
                String[] parts = actionParam.split(":", 2);
                if (parts.length < 2) yield "Invalid withdraw parameters.";
                yield recruitmentRenderer.handleWithdrawBid(parts[0], parts[1]);
            }
            case "recruit_bid" -> {
                yield recruitmentRenderer.handlePlaceBid(actionParam, "100");
            }
            default -> null;
        };

        if (error != null) {
            playerRef.sendMessage(com.hypixel.hytale.server.core.Message.raw(error).color("red"));
        }

        refreshCurrentView(store);
    }

    /**
     * Re-render the current view in-place using sendUpdate() to avoid full page rebuild flicker.
     */
    private void refreshCurrentView(Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        if (currentPath == null) {
            buildRootMenu(cmd, events);
        } else {
            String[] parts = currentPath.split(":");
            String module = parts[0];
            String view = parts.length > 1 ? parts[1] : null;
            String subView = parts.length > 2 ? parts[2] : null;
            buildModuleView(cmd, events, store, module, view, subView);
        }

        sendUpdate(cmd, events, false);
    }

    private void showModuleUnavailable(UICommandBuilder cmd, String moduleName) {
        cmd.set("#ContentText.Visible", true);
        cmd.set("#ContentText.Text", moduleName + " module is not available. The required plugin may not be installed.");
    }

    private boolean isModuleAvailable(String module) {
        return switch (module) {
            case "factions" -> HC_FactionsPlugin.getInstance() != null;
            case "honor" -> isPluginLoaded("com.hchonor.HC_HonorPlugin");
            case "attributes" -> isPluginLoaded("com.hcattributes.HC_AttributesPlugin");
            case "classes" -> isPluginLoaded("com.hcclasses.HC_ClassesPlugin");
            case "character" -> isPluginLoaded("com.hcattributes.HC_AttributesPlugin") || isPluginLoaded("com.hcclasses.HC_ClassesPlugin");
            case "skills" -> isPluginLoaded("com.hcprofessions.HC_ProfessionsPlugin");
            case "recruitment" -> isPluginLoaded("com.hcrecruitment.HC_RecruitmentPlugin");
            case "characters" -> isPluginLoaded("com.hcmultichar.HC_MultiCharPlugin");
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

    private void openCharacterSelect(Ref<EntityStore> ref, Store<EntityStore> store, Player player) {
        try {
            Class<?> pluginClass = Class.forName("com.hcmultichar.HC_MultiCharPlugin");
            Object multiCharPlugin = pluginClass.getMethod("getInstance").invoke(null);
            if (multiCharPlugin == null) return;

            Class<?> guiClass = Class.forName("com.hcmultichar.gui.CharacterSelectGui");
            var constructor = guiClass.getConstructor(pluginClass, PlayerRef.class);
            var gui = constructor.newInstance(multiCharPlugin, playerRef);

            player.getPageManager().openCustomPage(ref, store,
                (InteractiveCustomUIPage<?>) gui);
        } catch (Exception e) {
            playerRef.sendMessage(Message.raw("Character selection unavailable.").color(Color.RED));
        }
    }

    private String getPlayerSubtitle() {
        String factionTag = factionsRenderer.getFactionTag();
        if (factionTag != null) {
            return "[" + factionTag + "] " + playerRef.getUsername();
        }
        return playerRef.getUsername();
    }

    // ─── Player Data Helpers (reflection-based for optional plugin dependencies) ───

    private static String getPlayerLevel(UUID uuid) {
        try {
            Class<?> apiClass = Class.forName("com.hcleveling.api.HC_LevelingAPI");
            var api = apiClass.getMethod("getInstance").invoke(null);
            if (api != null) {
                return String.valueOf(apiClass.getMethod("getPlayerLevel", UUID.class).invoke(api, uuid));
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String getClassName(UUID uuid) {
        try {
            Class<?> apiClass = Class.forName("com.hcclasses.api.HC_ClassesAPI");
            var api = apiClass.getMethod("getInstance").invoke(null);
            if (api != null) {
                return (String) apiClass.getMethod("getPlayerClassName", UUID.class).invoke(api, uuid);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String getFactionName(UUID uuid) {
        try {
            Class<?> pluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
            var plugin = pluginClass.getMethod("getInstance").invoke(null);
            if (plugin != null) {
                var repo = pluginClass.getMethod("getPlayerDataRepository").invoke(plugin);
                var data = repo.getClass().getMethod("getPlayerData", UUID.class).invoke(repo, uuid);
                if (data != null) {
                    String factionId = (String) data.getClass().getMethod("getFactionId").invoke(data);
                    if (factionId != null) {
                        var manager = pluginClass.getMethod("getFactionManager").invoke(plugin);
                        var faction = manager.getClass().getMethod("getFaction", String.class).invoke(manager, factionId);
                        if (faction != null) {
                            return (String) faction.getClass().getMethod("getDisplayName").invoke(faction);
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
            var plugin = pluginClass.getMethod("getInstance").invoke(null);
            if (plugin != null) {
                var repo = pluginClass.getMethod("getPlayerDataRepository").invoke(plugin);
                var data = repo.getClass().getMethod("getPlayerData", UUID.class).invoke(repo, uuid);
                if (data != null) {
                    var guildId = (UUID) data.getClass().getMethod("getGuildId").invoke(data);
                    if (guildId != null) {
                        var guildManager = pluginClass.getMethod("getGuildManager").invoke(plugin);
                        var guild = guildManager.getClass().getMethod("getGuild", UUID.class).invoke(guildManager, guildId);
                        if (guild != null) {
                            return (String) guild.getClass().getMethod("getName").invoke(guild);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String getHonorRankTitle(UUID uuid) {
        try {
            Class<?> pluginClass = Class.forName("com.hchonor.HC_HonorPlugin");
            var plugin = pluginClass.getMethod("getInstance").invoke(null);
            if (plugin == null) return null;
            var manager = pluginClass.getMethod("getHonorManager").invoke(plugin);
            if (manager == null) return null;
            // Get faction ID for rank title
            String factionId = null;
            try {
                Class<?> factionPluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
                var factionPlugin = factionPluginClass.getMethod("getInstance").invoke(null);
                if (factionPlugin != null) {
                    var repo = factionPluginClass.getMethod("getPlayerDataRepository").invoke(factionPlugin);
                    var data = repo.getClass().getMethod("getPlayerData", UUID.class).invoke(repo, uuid);
                    if (data != null) {
                        factionId = (String) data.getClass().getMethod("getFactionId").invoke(data);
                    }
                }
            } catch (Exception ignored) {}
            // Get player's username for the honor data call
            var honorData = manager.getClass().getMethod("getHonorData", UUID.class, String.class)
                .invoke(manager, uuid, "");
            if (honorData == null) return null;
            var rank = honorData.getClass().getMethod("getRank").invoke(honorData);
            if (rank == null) return null;
            int rankNumber = (int) rank.getClass().getMethod("getRankNumber").invoke(rank);
            if (rankNumber <= 0) return null;
            String title = (String) rank.getClass().getMethod("getTitle", String.class).invoke(rank, factionId);
            return title;
        } catch (Exception ignored) {}
        return null;
    }

    private static String getHonorRankBadge(UUID uuid) {
        try {
            Class<?> pluginClass = Class.forName("com.hchonor.HC_HonorPlugin");
            var plugin = pluginClass.getMethod("getInstance").invoke(null);
            if (plugin == null) return null;
            var manager = pluginClass.getMethod("getHonorManager").invoke(plugin);
            if (manager == null) return null;
            var honorData = manager.getClass().getMethod("getHonorData", UUID.class, String.class)
                .invoke(manager, uuid, "");
            if (honorData == null) return null;
            var rank = honorData.getClass().getMethod("getRank").invoke(honorData);
            if (rank == null) return null;
            int rankNumber = (int) rank.getClass().getMethod("getRankNumber").invoke(rank);
            return rankNumber > 0 ? "R" + rankNumber : null;
        } catch (Exception ignored) {}
        return null;
    }

    private static String getCharacterCount(UUID uuid) {
        try {
            Class<?> apiClass = Class.forName("com.hcmultichar.api.HC_MultiCharAPI");
            Boolean isAvailable = (Boolean) apiClass.getMethod("isAvailable").invoke(null);
            if (!isAvailable) return null;
            @SuppressWarnings("unchecked")
            List<?> chars = (List<?>) apiClass.getMethod("getCharacters", UUID.class).invoke(null, uuid);
            int count = chars != null ? chars.size() : 0;
            int activeSlot = (int) apiClass.getMethod("getActiveSlot", UUID.class).invoke(null, uuid);
            return count > 0 ? (activeSlot + 1) + "/" + count : null;
        } catch (Exception ignored) {}
        return null;
    }

    private static String getMainProfession(UUID uuid) {
        try {
            Class<?> pluginClass = Class.forName("com.hcprofessions.HC_ProfessionsPlugin");
            var plugin = pluginClass.getMethod("getInstance").invoke(null);
            if (plugin == null) return null;
            var profManager = pluginClass.getMethod("getProfessionManager").invoke(plugin);
            if (profManager == null) return null;
            var profession = profManager.getClass().getMethod("getProfession", UUID.class).invoke(profManager, uuid);
            if (profession == null) return null;
            return (String) profession.getClass().getMethod("getDisplayName").invoke(profession);
        } catch (Exception ignored) {}
        return null;
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
