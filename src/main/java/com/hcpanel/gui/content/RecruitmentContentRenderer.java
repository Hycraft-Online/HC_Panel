package com.hcpanel.gui.content;

import com.hcpanel.gui.UnifiedPanelGui.SidebarButton;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

// HC_Factions is a required dependency of HC_Panel
import com.hcfactions.HC_FactionsPlugin;
import com.hcfactions.models.Guild;
import com.hcfactions.models.PlayerData;

import java.awt.Color;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Renders Recruitment module content into the unified panel.
 * Uses reflection for HC_Recruitment to make it fully optional at runtime.
 */
public class RecruitmentContentRenderer {

    private final PlayerRef playerRef;

    // Cached reflection references
    private static boolean reflectionInitialized = false;
    private static boolean recruitmentAvailable = false;

    // HC_RecruitmentPlugin
    private static Method getInstanceMethod;
    private static Method getRankManagerMethod;
    private static Method getBidManagerMethod;
    private static Method getBiddingWindowManagerMethod;

    // RankManager
    private static Method getRankMethod;
    private static Method getRankInfoMethod;

    // RankRepository (for getAvailableRecruits)
    private static Method getRankRepositoryMethod;
    private static Method getAvailableRecruitsMethod;

    // BidManager
    private static Method getActiveBidsForPlayerMethod;
    private static Method getActiveBidsForGuildMethod;
    private static Method acceptBidMethod;
    private static Method cancelBidMethod;
    private static Method placeBidMethod;

    // BiddingWindowManager
    private static Method hasOpenWindowMethod;
    private static Method getRemainingSecondsMethod;

    // PlayerRank enum
    private static Method rankGetDisplayMethod;
    private static Method rankGetColorHexMethod;

    // GuildBid
    private static Method bidGetBidIdMethod;
    private static Method bidGetGuildIdMethod;
    private static Method bidGetAmountMethod;
    private static Method bidGetBidderNameMethod;
    private static Method bidGetPlayerUuidMethod;

    // RankInfo record
    private static Method rankInfoPlayerUuidMethod;
    private static Method rankInfoPlayerNameMethod;
    private static Method rankInfoRankMethod;
    private static Method rankInfoBossMaxHealthMethod;
    private static Method rankInfoDamageDealtMethod;
    private static Method rankInfoDamagePercentMethod;
    private static Method rankInfoBossDefeatedMethod;

    public RecruitmentContentRenderer(PlayerRef playerRef) {
        this.playerRef = playerRef;
        initReflection();
    }

    private static synchronized void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;
        try {
            // Plugin class
            Class<?> pluginClass = Class.forName("com.hcrecruitment.HC_RecruitmentPlugin");
            getInstanceMethod = pluginClass.getMethod("getInstance");
            getRankManagerMethod = pluginClass.getMethod("getRankManager");
            getBidManagerMethod = pluginClass.getMethod("getBidManager");
            getBiddingWindowManagerMethod = pluginClass.getMethod("getBiddingWindowManager");

            // RankManager
            Class<?> rankManagerClass = Class.forName("com.hcrecruitment.managers.RankManager");
            getRankMethod = rankManagerClass.getMethod("getRank", UUID.class);
            getRankInfoMethod = rankManagerClass.getMethod("getRankInfo", UUID.class);
            getRankRepositoryMethod = rankManagerClass.getMethod("getRankRepository");

            // RankRepository
            Class<?> rankRepoClass = Class.forName("com.hcrecruitment.database.RankRepository");
            getAvailableRecruitsMethod = rankRepoClass.getMethod("getAvailableRecruits");

            // BidManager
            Class<?> bidManagerClass = Class.forName("com.hcrecruitment.managers.BidManager");
            getActiveBidsForPlayerMethod = bidManagerClass.getMethod("getActiveBidsForPlayer", UUID.class);
            getActiveBidsForGuildMethod = bidManagerClass.getMethod("getActiveBidsForGuild", UUID.class);
            acceptBidMethod = bidManagerClass.getMethod("acceptBid", UUID.class);
            cancelBidMethod = bidManagerClass.getMethod("cancelBid", UUID.class, UUID.class);
            placeBidMethod = bidManagerClass.getMethod("placeBid", UUID.class, UUID.class, UUID.class, String.class, double.class);

            // BiddingWindowManager
            Class<?> windowManagerClass = Class.forName("com.hcrecruitment.managers.BiddingWindowManager");
            hasOpenWindowMethod = windowManagerClass.getMethod("hasOpenWindow", UUID.class);
            getRemainingSecondsMethod = windowManagerClass.getMethod("getRemainingSeconds", UUID.class);

            // PlayerRank enum
            Class<?> playerRankClass = Class.forName("com.hcrecruitment.models.PlayerRank");
            rankGetDisplayMethod = playerRankClass.getMethod("getDisplay");
            rankGetColorHexMethod = playerRankClass.getMethod("getColorHex");

            // GuildBid
            Class<?> guildBidClass = Class.forName("com.hcrecruitment.models.GuildBid");
            bidGetBidIdMethod = guildBidClass.getMethod("getBidId");
            bidGetGuildIdMethod = guildBidClass.getMethod("getGuildId");
            bidGetAmountMethod = guildBidClass.getMethod("getAmount");
            bidGetBidderNameMethod = guildBidClass.getMethod("getBidderName");
            bidGetPlayerUuidMethod = guildBidClass.getMethod("getPlayerUuid");

            // RankInfo record
            Class<?> rankInfoClass = Class.forName("com.hcrecruitment.database.RankRepository$RankInfo");
            rankInfoPlayerUuidMethod = rankInfoClass.getMethod("playerUuid");
            rankInfoPlayerNameMethod = rankInfoClass.getMethod("playerName");
            rankInfoRankMethod = rankInfoClass.getMethod("rank");
            rankInfoBossMaxHealthMethod = rankInfoClass.getMethod("bossMaxHealth");
            rankInfoDamageDealtMethod = rankInfoClass.getMethod("damageDealt");
            rankInfoDamagePercentMethod = rankInfoClass.getMethod("damagePercent");
            rankInfoBossDefeatedMethod = rankInfoClass.getMethod("bossDefeated");

            recruitmentAvailable = true;
        } catch (Exception e) {
            recruitmentAvailable = false;
        }
    }

    public static boolean isRecruitmentPluginAvailable() {
        initReflection();
        if (!recruitmentAvailable) return false;
        try {
            return getInstanceMethod.invoke(null) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAvailable() {
        return isRecruitmentPluginAvailable();
    }

    // ═══════════════════════════════════════════════════════
    // MANAGER ACCESSORS
    // ═══════════════════════════════════════════════════════

    private Object getPlugin() {
        try { return getInstanceMethod.invoke(null); } catch (Exception e) { return null; }
    }

    private Object getRankManager() {
        try {
            Object plugin = getPlugin();
            return plugin != null ? getRankManagerMethod.invoke(plugin) : null;
        } catch (Exception e) { return null; }
    }

    private Object getBidManager() {
        try {
            Object plugin = getPlugin();
            return plugin != null ? getBidManagerMethod.invoke(plugin) : null;
        } catch (Exception e) { return null; }
    }

    private Object getWindowManager() {
        try {
            Object plugin = getPlugin();
            return plugin != null ? getBiddingWindowManagerMethod.invoke(plugin) : null;
        } catch (Exception e) { return null; }
    }

    // ═══════════════════════════════════════════════════════
    // REFLECTION HELPERS
    // ═══════════════════════════════════════════════════════

    private Object invokeGetRank(Object rankManager, UUID uuid) {
        try { return getRankMethod.invoke(rankManager, uuid); } catch (Exception e) { return null; }
    }

    private Object invokeGetRankInfo(Object rankManager, UUID uuid) {
        try { return getRankInfoMethod.invoke(rankManager, uuid); } catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private List<?> invokeGetAvailableRecruits(Object rankManager) {
        try {
            Object rankRepo = getRankRepositoryMethod.invoke(rankManager);
            return rankRepo != null ? (List<?>) getAvailableRecruitsMethod.invoke(rankRepo) : List.of();
        } catch (Exception e) { return List.of(); }
    }

    @SuppressWarnings("unchecked")
    private List<?> invokeGetActiveBidsForPlayer(Object bidManager, UUID uuid) {
        try { return (List<?>) getActiveBidsForPlayerMethod.invoke(bidManager, uuid); } catch (Exception e) { return List.of(); }
    }

    @SuppressWarnings("unchecked")
    private List<?> invokeGetActiveBidsForGuild(Object bidManager, UUID guildId) {
        try { return (List<?>) getActiveBidsForGuildMethod.invoke(bidManager, guildId); } catch (Exception e) { return List.of(); }
    }

    private boolean invokeHasOpenWindow(Object windowManager, UUID uuid) {
        try { return (boolean) hasOpenWindowMethod.invoke(windowManager, uuid); } catch (Exception e) { return false; }
    }

    private int invokeGetRemainingSeconds(Object windowManager, UUID uuid) {
        try { return (int) getRemainingSecondsMethod.invoke(windowManager, uuid); } catch (Exception e) { return -1; }
    }

    // PlayerRank helpers
    private String invokeRankDisplay(Object rank) {
        try { return (String) rankGetDisplayMethod.invoke(rank); } catch (Exception e) { return "?"; }
    }

    private String invokeRankColorHex(Object rank) {
        try { return (String) rankGetColorHexMethod.invoke(rank); } catch (Exception e) { return "#808080"; }
    }

    // GuildBid helpers
    private UUID invokeBidId(Object bid) {
        try { return (UUID) bidGetBidIdMethod.invoke(bid); } catch (Exception e) { return null; }
    }

    private UUID invokeBidGuildId(Object bid) {
        try { return (UUID) bidGetGuildIdMethod.invoke(bid); } catch (Exception e) { return null; }
    }

    private double invokeBidAmount(Object bid) {
        try { return (double) bidGetAmountMethod.invoke(bid); } catch (Exception e) { return 0; }
    }

    private String invokeBidderName(Object bid) {
        try { return (String) bidGetBidderNameMethod.invoke(bid); } catch (Exception e) { return "Unknown"; }
    }

    private UUID invokeBidPlayerUuid(Object bid) {
        try { return (UUID) bidGetPlayerUuidMethod.invoke(bid); } catch (Exception e) { return null; }
    }

    // RankInfo helpers
    private UUID invokeRankInfoPlayerUuid(Object info) {
        try { return (UUID) rankInfoPlayerUuidMethod.invoke(info); } catch (Exception e) { return null; }
    }

    private String invokeRankInfoPlayerName(Object info) {
        try { return (String) rankInfoPlayerNameMethod.invoke(info); } catch (Exception e) { return "Unknown"; }
    }

    private Object invokeRankInfoRank(Object info) {
        try { return rankInfoRankMethod.invoke(info); } catch (Exception e) { return null; }
    }

    private double invokeRankInfoBossMaxHealth(Object info) {
        try { return (double) rankInfoBossMaxHealthMethod.invoke(info); } catch (Exception e) { return 0; }
    }

    private double invokeRankInfoDamageDealt(Object info) {
        try { return (double) rankInfoDamageDealtMethod.invoke(info); } catch (Exception e) { return 0; }
    }

    private double invokeRankInfoDamagePercent(Object info) {
        try { return (double) rankInfoDamagePercentMethod.invoke(info); } catch (Exception e) { return 0; }
    }

    private boolean invokeRankInfoBossDefeated(Object info) {
        try { return (boolean) rankInfoBossDefeatedMethod.invoke(info); } catch (Exception e) { return false; }
    }

    // ═══════════════════════════════════════════════════════
    // FACTIONS HELPERS (direct API, required dependency)
    // ═══════════════════════════════════════════════════════

    private UUID getPlayerGuildId() {
        try {
            HC_FactionsPlugin factionsPlugin = HC_FactionsPlugin.getInstance();
            if (factionsPlugin == null) return null;
            PlayerData playerData = factionsPlugin.getPlayerDataRepository().getPlayerData(playerRef.getUuid());
            return playerData != null ? playerData.getGuildId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getGuildName(UUID guildId) {
        try {
            HC_FactionsPlugin factionsPlugin = HC_FactionsPlugin.getInstance();
            if (factionsPlugin == null) return "Unknown Guild";
            Guild guild = factionsPlugin.getGuildManager().getGuild(guildId);
            return guild != null ? guild.getName() : "Unknown Guild";
        } catch (Exception e) {
            return "Unknown Guild";
        }
    }

    private String getGuildTag(UUID guildId) {
        try {
            HC_FactionsPlugin factionsPlugin = HC_FactionsPlugin.getInstance();
            if (factionsPlugin == null) return null;
            Guild guild = factionsPlugin.getGuildManager().getGuild(guildId);
            return guild != null ? guild.getTag() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════
    // SIDEBAR
    // ═══════════════════════════════════════════════════════

    public List<SidebarButton> getSidebarButtons(String currentView) {
        List<SidebarButton> buttons = new ArrayList<>();
        boolean isRank = currentView == null || "rank".equals(currentView);
        boolean isBids = "bids".equals(currentView);
        boolean isBrowse = "browse".equals(currentView);
        boolean isGuildBids = "guildbids".equals(currentView);

        // Badge for bids count
        String bidBadge = getBidBadge();

        buttons.add(new SidebarButton("My Rank", "nav:recruitment:rank", null, "#ffd700", isRank));
        buttons.add(new SidebarButton("Bids on Me", "nav:recruitment:bids", bidBadge, "#ffd700", isBids));
        buttons.add(new SidebarButton("Browse Recruits", "nav:recruitment:browse", null, "#ffd700", isBrowse));
        buttons.add(new SidebarButton("Guild Bids", "nav:recruitment:guildbids", null, "#ffd700", isGuildBids));
        return buttons;
    }

    /**
     * Get badge text showing active bid count on this player.
     */
    public String getBidBadge() {
        if (!isAvailable()) return null;
        Object bidManager = getBidManager();
        if (bidManager == null) return null;
        List<?> bids = invokeGetActiveBidsForPlayer(bidManager, playerRef.getUuid());
        return bids.isEmpty() ? null : String.valueOf(bids.size());
    }

    // ═══════════════════════════════════════════════════════
    // CONTENT RENDERING
    // ═══════════════════════════════════════════════════════

    public void renderContent(UICommandBuilder cmd, UIEventBuilder events, String view) {
        if (!isAvailable()) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "Recruitment system not available.");
            return;
        }

        if (view == null) view = "rank";

        switch (view) {
            case "rank" -> renderRank(cmd, events);
            case "bids" -> renderBids(cmd, events);
            case "browse" -> renderBrowse(cmd, events);
            case "guildbids" -> renderGuildBids(cmd, events);
            default -> renderRank(cmd, events);
        }
    }

    // ═══════════════════════════════════════════════════════
    // VIEW: MY RANK
    // ═══════════════════════════════════════════════════════

    private void renderRank(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.set("#HeaderSection.Visible", false);

        Object rankManager = getRankManager();
        if (rankManager == null) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "Rank system not available.");
            return;
        }

        Object rankInfo = invokeGetRankInfo(rankManager, playerRef.getUuid());

        cmd.set("#RecruitRankSection.Visible", true);

        if (rankInfo == null) {
            // Unranked
            cmd.set("#RecruitRankEmblemBg.Background", "#333333");
            cmd.set("#RecruitRankLetter.TextSpans", Message.raw("?").color(Color.decode("#808080")));
            cmd.set("#RecruitRankTitle.TextSpans", Message.raw("UNRANKED").color(Color.decode("#808080")));
            cmd.set("#RecruitRankDesc.Text", "Complete the Asylum boss fight to receive your Hunter rank.");

            // Hide stats
            for (int i = 0; i < 4; i++) {
                cmd.set("#RecruitStatRow" + i + ".Visible", false);
            }
        } else {
            Object rank = invokeRankInfoRank(rankInfo);
            String display = invokeRankDisplay(rank);
            String colorHex = invokeRankColorHex(rank);
            boolean bossDefeated = invokeRankInfoBossDefeated(rankInfo);
            double damageDealt = invokeRankInfoDamageDealt(rankInfo);
            double damagePercent = invokeRankInfoDamagePercent(rankInfo);
            double bossMaxHealth = invokeRankInfoBossMaxHealth(rankInfo);

            // Emblem
            cmd.set("#RecruitRankEmblemBg.Background", colorHex);
            cmd.set("#RecruitRankLetter.TextSpans", Message.raw(display).color(Color.decode("#000000")));
            cmd.set("#RecruitRankTitle.TextSpans", Message.raw(display + "-RANK HUNTER").color(Color.decode(colorHex)));
            cmd.set("#RecruitRankDesc.Text", playerRef.getUsername());

            // Stats
            cmd.set("#RecruitStatRow0.Visible", true);
            cmd.set("#RecruitStatLabel0.Text", "Boss Defeated");
            cmd.set("#RecruitStatValue0.TextSpans", Message.raw(bossDefeated ? "Yes" : "No").color(Color.decode(bossDefeated ? "#4aff7f" : "#e74c3c")));

            cmd.set("#RecruitStatRow1.Visible", true);
            cmd.set("#RecruitStatLabel1.Text", "Damage Dealt");
            cmd.set("#RecruitStatValue1.Text", String.format("%,.0f", damageDealt));

            cmd.set("#RecruitStatRow2.Visible", true);
            cmd.set("#RecruitStatLabel2.Text", "Damage %");
            cmd.set("#RecruitStatValue2.TextSpans", Message.raw(String.format("%.1f%%", damagePercent)).color(Color.decode(colorHex)));

            cmd.set("#RecruitStatRow3.Visible", true);
            cmd.set("#RecruitStatLabel3.Text", "Boss Max HP");
            cmd.set("#RecruitStatValue3.Text", String.format("%,.0f", bossMaxHealth));
        }

        // Rank tier legend
        cmd.set("#RecruitTierLegend.Visible", true);
        cmd.set("#FooterText.Text", "Rank is based on your performance in the Asylum boss fight.");
    }

    // ═══════════════════════════════════════════════════════
    // VIEW: BIDS ON ME
    // ═══════════════════════════════════════════════════════

    private void renderBids(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.set("#HeaderSection.Visible", false);

        Object bidManager = getBidManager();
        Object windowManager = getWindowManager();
        if (bidManager == null) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "Bid system not available.");
            return;
        }

        cmd.set("#RecruitBidsSection.Visible", true);

        // Window timer
        if (windowManager != null && invokeHasOpenWindow(windowManager, playerRef.getUuid())) {
            int remaining = invokeGetRemainingSeconds(windowManager, playerRef.getUuid());
            if (remaining > 0) {
                int mins = remaining / 60;
                int secs = remaining % 60;
                cmd.set("#RecruitWindowTimer.TextSpans", Message.raw(String.format("Bidding window open: %d:%02d remaining", mins, secs)).color(Color.decode("#4aff7f")));
            } else {
                cmd.set("#RecruitWindowTimer.TextSpans", Message.raw("Bidding window expired").color(Color.decode("#777777")));
            }
        } else {
            cmd.set("#RecruitWindowTimer.TextSpans", Message.raw("No active bidding window").color(Color.decode("#777777")));
        }

        // Bid list
        List<?> bids = invokeGetActiveBidsForPlayer(bidManager, playerRef.getUuid());

        if (bids.isEmpty()) {
            cmd.set("#RecruitBidsEmpty.Visible", true);
            cmd.set("#RecruitBidsEmpty.Text", "No active bids on you.");
        } else {
            cmd.set("#RecruitBidsEmpty.Visible", false);

            int row = 0;
            for (Object bid : bids) {
                if (row >= 8) break;

                UUID guildId = invokeBidGuildId(bid);
                String guildName = getGuildName(guildId);
                String guildTag = getGuildTag(guildId);
                double amount = invokeBidAmount(bid);
                String bidderName = invokeBidderName(bid);
                UUID bidId = invokeBidId(bid);

                String label = guildTag != null
                    ? "[" + guildTag + "] " + guildName
                    : guildName;

                cmd.set("#RecruitBidRow" + row + ".Visible", true);
                cmd.set("#RecruitBidGuild" + row + ".Text", label);
                cmd.set("#RecruitBidAmount" + row + ".TextSpans", Message.raw(String.format("%,.0f gold", amount)).color(Color.decode("#ffd700")));
                cmd.set("#RecruitBidBy" + row + ".Text", "by " + bidderName);

                // Accept button
                events.addEventBinding(CustomUIEventBindingType.Activating, "#RecruitBidAccept" + row,
                    EventData.of("Action", "action:recruit_accept:" + bidId.toString()), false);

                // Decline button
                events.addEventBinding(CustomUIEventBindingType.Activating, "#RecruitBidDecline" + row,
                    EventData.of("Action", "action:recruit_decline:" + guildId.toString()), false);

                row++;
            }

            // Hide remaining rows
            for (int i = row; i < 8; i++) {
                cmd.set("#RecruitBidRow" + i + ".Visible", false);
            }
        }

        cmd.set("#FooterText.Text", "Accept a bid to join that guild. Declined bids refund the guild's gold.");
    }

    // ═══════════════════════════════════════════════════════
    // VIEW: BROWSE RECRUITS
    // ═══════════════════════════════════════════════════════

    private void renderBrowse(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.set("#HeaderSection.Visible", false);

        Object rankManager = getRankManager();
        Object windowManager = getWindowManager();
        if (rankManager == null) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "Recruitment system not available.");
            return;
        }

        cmd.set("#RecruitBrowseSection.Visible", true);

        List<?> recruits = invokeGetAvailableRecruits(rankManager);

        cmd.set("#RecruitBrowseCount.Text", "Available Recruits: " + recruits.size());

        if (recruits.isEmpty()) {
            cmd.set("#RecruitBrowseEmpty.Visible", true);
            cmd.set("#RecruitBrowseEmpty.Text", "No ranked players available for recruitment.");
        } else {
            cmd.set("#RecruitBrowseEmpty.Visible", false);

            int row = 0;
            for (Object info : recruits) {
                if (row >= 10) break;

                String name = invokeRankInfoPlayerName(info);
                Object rank = invokeRankInfoRank(info);
                String rankDisplay = invokeRankDisplay(rank);
                String rankColor = invokeRankColorHex(rank);
                UUID recruitUuid = invokeRankInfoPlayerUuid(info);

                // Check window status
                boolean hasWindow = windowManager != null && invokeHasOpenWindow(windowManager, recruitUuid);

                cmd.set("#RecruitBrowseRow" + row + ".Visible", true);
                cmd.set("#RecruitBrowseRank" + row + ".TextSpans", Message.raw(rankDisplay).color(Color.decode(rankColor)));
                cmd.set("#RecruitBrowseName" + row + ".Text", name);
                cmd.set("#RecruitBrowseStatus" + row + ".TextSpans", Message.raw(hasWindow ? "OPEN" : "").color(Color.decode(hasWindow ? "#4aff7f" : "#555555")));

                // Bid button (only useful for guild officers, but show for all)
                events.addEventBinding(CustomUIEventBindingType.Activating, "#RecruitBrowseBid" + row,
                    EventData.of("Action", "action:recruit_bid:" + recruitUuid.toString()), false);

                row++;
            }

            // Hide remaining rows
            for (int i = row; i < 10; i++) {
                cmd.set("#RecruitBrowseRow" + i + ".Visible", false);
            }
        }

        cmd.set("#FooterText.Text", "Use /recruit bid <player> <amount> for custom amounts.");
    }

    // ═══════════════════════════════════════════════════════
    // VIEW: GUILD BIDS (outgoing)
    // ═══════════════════════════════════════════════════════

    private void renderGuildBids(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.set("#HeaderSection.Visible", false);

        Object bidManager = getBidManager();
        if (bidManager == null) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "Bid system not available.");
            return;
        }

        UUID guildId = getPlayerGuildId();

        cmd.set("#RecruitGuildBidsSection.Visible", true);

        if (guildId == null) {
            cmd.set("#RecruitGuildBidsTitle.Text", "GUILD BIDS");
            cmd.set("#RecruitGuildBidsEmpty.Visible", true);
            cmd.set("#RecruitGuildBidsEmpty.Text", "You are not in a guild.");
        } else {
            String guildName = getGuildName(guildId);
            cmd.set("#RecruitGuildBidsTitle.Text", guildName + " -- Active Bids");

            List<?> bids = invokeGetActiveBidsForGuild(bidManager, guildId);

            if (bids.isEmpty()) {
                cmd.set("#RecruitGuildBidsEmpty.Visible", true);
                cmd.set("#RecruitGuildBidsEmpty.Text", "No active outgoing bids.");
            } else {
                cmd.set("#RecruitGuildBidsEmpty.Visible", false);

                Object rankManager = getRankManager();
                int row = 0;
                for (Object bid : bids) {
                    if (row >= 8) break;

                    UUID recruitUuid = invokeBidPlayerUuid(bid);
                    double amount = invokeBidAmount(bid);
                    String bidderName = invokeBidderName(bid);

                    // Get recruit rank info
                    String recruitName = "Unknown";
                    String rankDisplay = "?";
                    String rankColor = "#808080";
                    if (rankManager != null) {
                        Object rankInfo = invokeGetRankInfo(rankManager, recruitUuid);
                        if (rankInfo != null) {
                            recruitName = invokeRankInfoPlayerName(rankInfo);
                            Object rank = invokeRankInfoRank(rankInfo);
                            rankDisplay = invokeRankDisplay(rank);
                            rankColor = invokeRankColorHex(rank);
                        }
                    }

                    cmd.set("#RecruitGuildBidRow" + row + ".Visible", true);
                    cmd.set("#RecruitGuildBidRank" + row + ".TextSpans", Message.raw(rankDisplay).color(Color.decode(rankColor)));
                    cmd.set("#RecruitGuildBidName" + row + ".Text", recruitName);
                    cmd.set("#RecruitGuildBidAmount" + row + ".TextSpans", Message.raw(String.format("%,.0f gold", amount)).color(Color.decode("#ffd700")));
                    cmd.set("#RecruitGuildBidBy" + row + ".Text", "by " + bidderName);

                    // Withdraw button
                    events.addEventBinding(CustomUIEventBindingType.Activating, "#RecruitGuildBidWithdraw" + row,
                        EventData.of("Action", "action:recruit_withdraw:" + guildId.toString() + ":" + recruitUuid.toString()), false);

                    row++;
                }

                // Hide remaining rows
                for (int i = row; i < 8; i++) {
                    cmd.set("#RecruitGuildBidRow" + i + ".Visible", false);
                }
            }
        }

        cmd.set("#FooterText.Text", "Withdrawing a bid refunds the escrowed gold to your guild bank.");
    }

    // ═══════════════════════════════════════════════════════
    // ACTION HANDLERS
    // ═══════════════════════════════════════════════════════

    /**
     * Handle accepting a bid.
     * @return null on success, error message on failure
     */
    public String handleAcceptBid(String bidIdStr) {
        try {
            Object bidManager = getBidManager();
            if (bidManager == null) return "Bid system not available.";
            UUID bidId = UUID.fromString(bidIdStr);
            return (String) acceptBidMethod.invoke(bidManager, bidId);
        } catch (Exception e) {
            return "Failed to accept bid: " + e.getMessage();
        }
    }

    /**
     * Handle declining a bid (cancel from guild perspective using guild+player).
     * @return null on success, error message on failure
     */
    public String handleDeclineBid(String guildIdStr) {
        try {
            Object bidManager = getBidManager();
            if (bidManager == null) return "Bid system not available.";
            UUID guildId = UUID.fromString(guildIdStr);
            return (String) cancelBidMethod.invoke(bidManager, guildId, playerRef.getUuid());
        } catch (Exception e) {
            return "Failed to decline bid: " + e.getMessage();
        }
    }

    /**
     * Handle withdrawing a guild's bid.
     * @return null on success, error message on failure
     */
    public String handleWithdrawBid(String guildIdStr, String playerUuidStr) {
        try {
            Object bidManager = getBidManager();
            if (bidManager == null) return "Bid system not available.";
            UUID guildId = UUID.fromString(guildIdStr);
            UUID playerUuid = UUID.fromString(playerUuidStr);
            return (String) cancelBidMethod.invoke(bidManager, guildId, playerUuid);
        } catch (Exception e) {
            return "Failed to withdraw bid: " + e.getMessage();
        }
    }

    /**
     * Handle placing a bid from the panel (quick bid with default amount).
     * @return null on success, error message on failure
     */
    public String handlePlaceBid(String recruitUuidStr, String amountStr) {
        try {
            Object bidManager = getBidManager();
            if (bidManager == null) return "Bid system not available.";

            UUID guildId = getPlayerGuildId();
            if (guildId == null) return "You are not in a guild.";

            UUID recruitUuid = UUID.fromString(recruitUuidStr);
            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                return "Invalid bid amount.";
            }

            return (String) placeBidMethod.invoke(bidManager, guildId, recruitUuid,
                playerRef.getUuid(), playerRef.getUsername(), amount);
        } catch (Exception e) {
            return "Failed to place bid: " + e.getMessage();
        }
    }
}
