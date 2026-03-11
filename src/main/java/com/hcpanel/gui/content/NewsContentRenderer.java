package com.hcpanel.gui.content;

import com.hcpanel.config.NewsConfig;
import com.hcpanel.config.NewsConfig.NewsEntry;
import com.hcpanel.gui.UnifiedPanelGui.SidebarButton;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Renders News content into the unified panel.
 * Displays news entries from the NewsConfig plus contextual action items
 * that make the home screen a portal to the rest of the game.
 */
public class NewsContentRenderer {

    private static final int MAX_QUICK_ACTIONS = 4;

    private final PlayerRef playerRef;
    private final NewsConfig newsConfig;

    public NewsContentRenderer(PlayerRef playerRef, NewsConfig newsConfig) {
        this.playerRef = playerRef;
        this.newsConfig = newsConfig;
    }

    public List<SidebarButton> getSidebarButtons(String currentView) {
        return new ArrayList<>();
    }

    /**
     * Renders news content with contextual action items.
     */
    public void renderContent(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.set("#ContentSpacer.Visible", false);
        cmd.set("#NewsSection.Visible", true);

        // Build and render contextual quick actions
        List<ActionItem> actions = buildQuickActions();
        if (!actions.isEmpty()) {
            cmd.set("#QuickActionsCard.Visible", true);
            cmd.set("#QuickActionsSpacer.Visible", true);
            for (int i = 0; i < MAX_QUICK_ACTIONS; i++) {
                if (i < actions.size()) {
                    ActionItem item = actions.get(i);
                    cmd.set("#QuickAction" + i + ".Visible", true);
                    cmd.set("#QuickAction" + i + ".TextSpans",
                        Message.raw(item.icon + "  ").color(Color.decode(item.color))
                            .insert(Message.raw(item.text).color(Color.decode("#e2e8f0"))));
                    // Bind click to navigate if this action has a target
                    if (item.navAction != null) {
                        events.addEventBinding(CustomUIEventBindingType.Activating, "#QuickAction" + i,
                            EventData.of("Action", item.navAction), false);
                    }
                } else {
                    cmd.set("#QuickAction" + i + ".Visible", false);
                }
            }
        }

        // Render news entries
        List<NewsEntry> entries = newsConfig.getEntries();
        for (int i = 0; i < 10; i++) {
            String cardId = "#NewsCard" + i;
            if (i < entries.size()) {
                NewsEntry entry = entries.get(i);
                cmd.set(cardId + ".Visible", true);
                cmd.set("#NewsBorder" + i + ".Background", entry.getTypeColor());
                cmd.set("#NewsType" + i + ".TextSpans", Message.raw(entry.getTypeLabel()).color(Color.decode(entry.getTypeColor())));
                cmd.set("#NewsTitle" + i + ".Text", entry.getTitle());
                cmd.set("#NewsDate" + i + ".Text", entry.getDate());
                cmd.set("#NewsContent" + i + ".Text", entry.getContent());
            } else {
                cmd.set(cardId + ".Visible", false);
            }
        }

        // Dynamic footer based on player state
        cmd.set("#FooterText.Text", buildDynamicFooter());
    }

    // ─── Quick Action Items ───

    private record ActionItem(String icon, String text, String color, String navAction) {
        ActionItem(String icon, String text, String color) {
            this(icon, text, color, null);
        }
    }

    private List<ActionItem> buildQuickActions() {
        List<ActionItem> actions = new ArrayList<>();
        UUID uuid = playerRef.getUuid();

        // Check unspent stat points
        int statPoints = getUnspentStatPoints(uuid);
        if (statPoints > 0) {
            actions.add(new ActionItem("+", statPoints + " unspent stat point" + (statPoints > 1 ? "s" : "") + " -- click to allocate", "#ffd700", "nav:character:allocate"));
        }

        // Check unspent talent points
        int talentPoints = getUnspentTalentPoints(uuid);
        if (talentPoints > 0) {
            actions.add(new ActionItem("*", talentPoints + " unspent talent point" + (talentPoints > 1 ? "s" : "") + " -- click to spend", "#8b5cf6", "nav:character:talents"));
        }

        // Check guild invitations
        int invites = getPendingInvitations(uuid);
        if (invites > 0) {
            actions.add(new ActionItem("!", invites + " pending guild invitation" + (invites > 1 ? "s" : "") + " -- click to review", "#4a9eff", "nav:factions:invitations"));
        }

        // Check recruitment bids
        int bids = getPendingBids(uuid);
        if (bids > 0) {
            actions.add(new ActionItem("$", bids + " recruitment bid" + (bids > 1 ? "s" : "") + " -- click to review", "#ffd700", "nav:recruitment:bids"));
        }

        // Check if bidding window is open
        if (hasBiddingWindow(uuid)) {
            actions.add(new ActionItem("!", "Your bidding window is OPEN -- click to view bids", "#4aff7f", "nav:recruitment:bids"));
        }

        // Check if no faction (critical first step — no panel nav, uses /faction command)
        if (actions.size() < MAX_QUICK_ACTIONS && !hasFaction(uuid)) {
            actions.add(new ActionItem("!", "No faction chosen -- use /faction to pick a side", "#ff6b6b"));
        }

        // Check if no guild
        if (actions.size() < MAX_QUICK_ACTIONS && hasFaction(uuid) && !hasGuild(uuid)) {
            actions.add(new ActionItem("G", "You have no guild -- click to browse guilds", "#4a9eff", "nav:factions:browser"));
        }

        // Check if no class (no panel nav, uses /class command)
        if (actions.size() < MAX_QUICK_ACTIONS && getClassName(uuid) == null) {
            actions.add(new ActionItem("?", "No class selected -- use /class choose to pick your class", "#ff6b6b"));
        }

        // Check if no main profession
        if (actions.size() < MAX_QUICK_ACTIONS && getMainProfession(uuid) == null) {
            actions.add(new ActionItem("#", "No main profession -- click to view skills", "#4ecdc4", "nav:skills:professions"));
        }

        // Show XP progress toward next level if not maxed and has room
        if (actions.size() < MAX_QUICK_ACTIONS) {
            String xpHint = getXpProgressHint(uuid);
            if (xpHint != null) {
                actions.add(new ActionItem("^", xpHint, "#4ecdc4", "nav:character:overview"));
            }
        }

        // Trim to max
        if (actions.size() > MAX_QUICK_ACTIONS) {
            actions = new ArrayList<>(actions.subList(0, MAX_QUICK_ACTIONS));
        }

        return actions;
    }

    private String buildDynamicFooter() {
        UUID uuid = playerRef.getUuid();

        if (!hasFaction(uuid)) {
            return "Choose a faction first! Use /faction to pick a side and begin your journey.";
        }

        int statPoints = getUnspentStatPoints(uuid);
        int talentPoints = getUnspentTalentPoints(uuid);
        if (statPoints > 0 || talentPoints > 0) {
            return "You have unspent points! Open Character in the sidebar to allocate them.";
        }

        int invites = getPendingInvitations(uuid);
        if (invites > 0) {
            return "You have guild invitations waiting. Open Guild to review them.";
        }

        int bids = getPendingBids(uuid);
        if (bids > 0) {
            return "Guilds want to recruit you! Check Recruitment > Bids on Me.";
        }

        if (getClassName(uuid) == null) {
            return "No class selected yet. Use /class to choose your class and unlock talents.";
        }

        if (!hasGuild(uuid)) {
            return "You have no guild. Open Guild in the sidebar to create or join one.";
        }

        // Show XP progress in footer for engaged players
        String xpHint = getXpProgressHint(uuid);
        if (xpHint != null) {
            return xpHint + ". Use the sidebar to explore all systems.";
        }

        return "Use the sidebar to manage your character, guild, honor, skills, and more.";
    }

    // ─── Reflection Helpers ───

    private static int getUnspentStatPoints(UUID uuid) {
        try {
            Class<?> apiClass = Class.forName("com.hcattributes.api.HC_AttributesAPI");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            if (api == null) return 0;
            Object data = apiClass.getMethod("getPlayerData", UUID.class).invoke(api, uuid);
            if (data == null) return 0;
            return (int) data.getClass().getMethod("getUnspentStatPoints").invoke(data);
        } catch (Exception e) { return 0; }
    }

    private static int getUnspentTalentPoints(UUID uuid) {
        try {
            Class<?> apiClass = Class.forName("com.hcclasses.api.HC_ClassesAPI");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            if (api == null) return 0;
            return (int) apiClass.getMethod("getUnspentTalentPoints", UUID.class).invoke(api, uuid);
        } catch (Exception e) { return 0; }
    }

    private static int getPendingInvitations(UUID uuid) {
        try {
            Class<?> pluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
            Object plugin = pluginClass.getMethod("getInstance").invoke(null);
            if (plugin == null) return 0;
            Object inviteManager = pluginClass.getMethod("getGuildInvitationManager").invoke(plugin);
            if (inviteManager == null) return 0;
            List<?> invites = (List<?>) inviteManager.getClass()
                .getMethod("getInvitationsForPlayer", UUID.class).invoke(inviteManager, uuid);
            return invites != null ? invites.size() : 0;
        } catch (Exception e) { return 0; }
    }

    private static int getPendingBids(UUID uuid) {
        try {
            Class<?> pluginClass = Class.forName("com.hcrecruitment.HC_RecruitmentPlugin");
            Object plugin = pluginClass.getMethod("getInstance").invoke(null);
            if (plugin == null) return 0;
            Object bidManager = pluginClass.getMethod("getBidManager").invoke(plugin);
            if (bidManager == null) return 0;
            List<?> bids = (List<?>) bidManager.getClass()
                .getMethod("getActiveBidsForPlayer", UUID.class).invoke(bidManager, uuid);
            return bids != null ? bids.size() : 0;
        } catch (Exception e) { return 0; }
    }

    private static boolean hasBiddingWindow(UUID uuid) {
        try {
            Class<?> pluginClass = Class.forName("com.hcrecruitment.HC_RecruitmentPlugin");
            Object plugin = pluginClass.getMethod("getInstance").invoke(null);
            if (plugin == null) return false;
            Object windowManager = pluginClass.getMethod("getBiddingWindowManager").invoke(plugin);
            if (windowManager == null) return false;
            return (boolean) windowManager.getClass()
                .getMethod("hasOpenWindow", UUID.class).invoke(windowManager, uuid);
        } catch (Exception e) { return false; }
    }

    private static boolean hasFaction(UUID uuid) {
        try {
            Class<?> pluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
            Object plugin = pluginClass.getMethod("getInstance").invoke(null);
            if (plugin == null) return true; // Don't nag if plugin missing
            Object repo = pluginClass.getMethod("getPlayerDataRepository").invoke(plugin);
            Object data = repo.getClass().getMethod("getPlayerData", UUID.class).invoke(repo, uuid);
            if (data == null) return false;
            Object factionId = data.getClass().getMethod("getFactionId").invoke(data);
            return factionId != null;
        } catch (Exception e) { return true; }
    }

    private static boolean hasGuild(UUID uuid) {
        try {
            Class<?> pluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
            Object plugin = pluginClass.getMethod("getInstance").invoke(null);
            if (plugin == null) return true; // Don't nag if plugin missing
            Object repo = pluginClass.getMethod("getPlayerDataRepository").invoke(plugin);
            Object data = repo.getClass().getMethod("getPlayerData", UUID.class).invoke(repo, uuid);
            if (data == null) return false;
            Object guildId = data.getClass().getMethod("getGuildId").invoke(data);
            return guildId != null;
        } catch (Exception e) { return true; }
    }

    private static String getClassName(UUID uuid) {
        try {
            Class<?> apiClass = Class.forName("com.hcclasses.api.HC_ClassesAPI");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            if (api == null) return null;
            return (String) apiClass.getMethod("getPlayerClassName", UUID.class).invoke(api, uuid);
        } catch (Exception e) { return null; }
    }

    private static String getXpProgressHint(UUID uuid) {
        try {
            Class<?> pluginClass = Class.forName("com.hcleveling.HC_LevelingPlugin");
            Object plugin = pluginClass.getMethod("getInstance").invoke(null);
            if (plugin == null) return null;
            Object levelManager = pluginClass.getMethod("getLevelManager").invoke(plugin);
            if (levelManager == null) return null;
            Class<?> apiClass = Class.forName("com.hcleveling.api.HC_LevelingAPI");
            Boolean isAvailable = (Boolean) apiClass.getMethod("isAvailable").invoke(null);
            if (!isAvailable) return null;
            int level = (int) apiClass.getMethod("getPlayerLevel", UUID.class).invoke(null, uuid);
            int maxLevel = (int) levelManager.getClass().getMethod("getMaxLevel").invoke(levelManager);
            if (level >= maxLevel) return null;
            // Get XP details from LevelManager via getPlayerData(UUID, String)
            Object playerData = null;
            try {
                playerData = levelManager.getClass().getMethod("getPlayerData", UUID.class, String.class).invoke(levelManager, uuid, "");
            } catch (NoSuchMethodException e) {
                return "Level " + level + " -- earn XP from kills and quests to level up";
            }
            if (playerData == null) return "Level " + level + " -- earn XP from kills and quests to level up";
            long currentXp = (long) playerData.getClass().getMethod("getCurrentXp").invoke(playerData);
            long xpForCurrent = (long) levelManager.getClass().getMethod("getXpForLevel", int.class).invoke(levelManager, level);
            long xpForNext = (long) levelManager.getClass().getMethod("getXpForLevel", int.class).invoke(levelManager, level + 1);
            long xpInLevel = currentXp - xpForCurrent;
            long xpNeeded = xpForNext - xpForCurrent;
            int percent = xpNeeded > 0 ? (int) ((xpInLevel * 100) / xpNeeded) : 0;
            return String.format("Level %d -- %d%% to Level %d (%,d / %,d XP)", level, percent, level + 1, xpInLevel, xpNeeded);
        } catch (Exception e) { return null; }
    }

    private static String getMainProfession(UUID uuid) {
        try {
            Class<?> pluginClass = Class.forName("com.hcprofessions.HC_ProfessionsPlugin");
            Object plugin = pluginClass.getMethod("getInstance").invoke(null);
            if (plugin == null) return null;
            Object profManager = pluginClass.getMethod("getProfessionManager").invoke(plugin);
            if (profManager == null) return null;
            Object profession = profManager.getClass()
                .getMethod("getProfession", UUID.class).invoke(profManager, uuid);
            return profession != null ? (String) profession.getClass().getMethod("getDisplayName").invoke(profession) : null;
        } catch (Exception e) { return null; }
    }
}
