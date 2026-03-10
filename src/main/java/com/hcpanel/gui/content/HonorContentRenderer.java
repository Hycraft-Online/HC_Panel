package com.hcpanel.gui.content;

import com.hcpanel.gui.UnifiedPanelGui.SidebarButton;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

// Direct imports from HC_Factions (optional dependency)
import com.hcfactions.HC_FactionsPlugin;
import com.hcfactions.models.PlayerData;

import com.hypixel.hytale.server.core.Message;

import java.awt.Color;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Renders Honor module content into the unified panel.
 * Uses reflection for HC_Honor to make it fully optional at runtime.
 */
public class HonorContentRenderer {

    private final PlayerRef playerRef;

    // Cached reflection references
    private static boolean reflectionInitialized = false;
    private static boolean honorAvailable = false;
    private static Method getInstanceMethod;
    private static Method getHonorManagerMethod;
    private static Method getHonorDataMethod;
    private static Method getBracketNumberMethod;
    private static Method calculateBracketRPMethod;
    private static Method getHonorToNextBracketMethod;
    private static Method getWeeklyLeaderboardMethod;
    private static Method getRankPointsLeaderboardMethod;
    private static Method getLifetimeLeaderboardMethod;
    // HonorData methods
    private static Method getRankMethod;
    private static Method getRankPointsMethod;
    private static Method getWeeklyHonorMethod;
    private static Method getWeeklyKillsMethod;
    private static Method getLifetimeKillsMethod;
    private static Method getCurrentHonorMethod;
    private static Method getPlayerNameMethod;
    // HonorRank methods
    private static Method getRankNumberMethod;
    private static Method getTitleMethod;
    private static Method valuesMethod;

    public HonorContentRenderer(PlayerRef playerRef) {
        this.playerRef = playerRef;
        initReflection();
    }

    private static synchronized void initReflection() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;
        try {
            Class<?> pluginClass = Class.forName("com.hchonor.HC_HonorPlugin");
            getInstanceMethod = pluginClass.getMethod("getInstance");
            getHonorManagerMethod = pluginClass.getMethod("getHonorManager");

            Class<?> managerClass = Class.forName("com.hchonor.managers.HonorManager");
            getHonorDataMethod = managerClass.getMethod("getHonorData", UUID.class, String.class);
            getBracketNumberMethod = managerClass.getMethod("getBracketNumber", int.class);
            calculateBracketRPMethod = managerClass.getMethod("calculateBracketRP", int.class);
            getHonorToNextBracketMethod = managerClass.getMethod("getHonorToNextBracket", int.class);
            getWeeklyLeaderboardMethod = managerClass.getMethod("getWeeklyLeaderboard", int.class);
            getRankPointsLeaderboardMethod = managerClass.getMethod("getRankPointsLeaderboard", int.class);
            getLifetimeLeaderboardMethod = managerClass.getMethod("getLifetimeLeaderboard", int.class);

            Class<?> dataClass = Class.forName("com.hchonor.models.HonorData");
            getRankMethod = dataClass.getMethod("getRank");
            getRankPointsMethod = dataClass.getMethod("getRankPoints");
            getWeeklyHonorMethod = dataClass.getMethod("getWeeklyHonor");
            getWeeklyKillsMethod = dataClass.getMethod("getWeeklyKills");
            getLifetimeKillsMethod = dataClass.getMethod("getLifetimeKills");
            getCurrentHonorMethod = dataClass.getMethod("getCurrentHonor");
            getPlayerNameMethod = dataClass.getMethod("getPlayerName");

            Class<?> rankClass = Class.forName("com.hchonor.models.HonorRank");
            getRankNumberMethod = rankClass.getMethod("getRankNumber");
            getTitleMethod = rankClass.getMethod("getTitle", String.class);
            valuesMethod = rankClass.getMethod("values");

            honorAvailable = true;
        } catch (Exception e) {
            honorAvailable = false;
        }
    }

    /**
     * Static check if HC_Honor plugin is available (used by UnifiedPanelGui).
     */
    public static boolean isHonorPluginAvailable() {
        initReflection();
        if (!honorAvailable) return false;
        try {
            return getInstanceMethod.invoke(null) != null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isHonorAvailable() {
        return isHonorPluginAvailable();
    }

    private Object getHonorManager() {
        try {
            Object plugin = getInstanceMethod.invoke(null);
            return plugin != null ? getHonorManagerMethod.invoke(plugin) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Object getHonorData() {
        try {
            Object manager = getHonorManager();
            return manager != null ? getHonorDataMethod.invoke(manager, playerRef.getUuid(), playerRef.getUsername()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Helper methods to call HonorData/HonorRank methods via reflection
    private Object invokeRank(Object honorData) {
        try { return getRankMethod.invoke(honorData); } catch (Exception e) { return null; }
    }
    private int invokeRankPoints(Object honorData) {
        try { return (int) getRankPointsMethod.invoke(honorData); } catch (Exception e) { return 0; }
    }
    private int invokeWeeklyHonor(Object honorData) {
        try { return (int) getWeeklyHonorMethod.invoke(honorData); } catch (Exception e) { return 0; }
    }
    private int invokeWeeklyKills(Object honorData) {
        try { return (int) getWeeklyKillsMethod.invoke(honorData); } catch (Exception e) { return 0; }
    }
    private int invokeLifetimeKills(Object honorData) {
        try { return (int) getLifetimeKillsMethod.invoke(honorData); } catch (Exception e) { return 0; }
    }
    private int invokeCurrentHonor(Object honorData) {
        try { return (int) getCurrentHonorMethod.invoke(honorData); } catch (Exception e) { return 0; }
    }
    private String invokePlayerName(Object honorData) {
        try { return (String) getPlayerNameMethod.invoke(honorData); } catch (Exception e) { return "Unknown"; }
    }
    private int invokeRankNumber(Object rank) {
        try { return (int) getRankNumberMethod.invoke(rank); } catch (Exception e) { return 0; }
    }
    private String invokeTitle(Object rank, String factionId) {
        try { return (String) getTitleMethod.invoke(rank, factionId); } catch (Exception e) { return "Unknown"; }
    }
    private int invokeBracketNumber(Object manager, int weeklyHonor) {
        try { return (int) getBracketNumberMethod.invoke(manager, weeklyHonor); } catch (Exception e) { return 0; }
    }
    private int invokeCalculateBracketRP(Object manager, int weeklyHonor) {
        try { return (int) calculateBracketRPMethod.invoke(manager, weeklyHonor); } catch (Exception e) { return 0; }
    }
    private int invokeGetHonorToNextBracket(Object manager, int weeklyHonor) {
        try { return (int) getHonorToNextBracketMethod.invoke(manager, weeklyHonor); } catch (Exception e) { return 0; }
    }

    private String getFactionId() {
        try {
            HC_FactionsPlugin factionsPlugin = HC_FactionsPlugin.getInstance();
            if (factionsPlugin == null) return null;
            PlayerData playerData = factionsPlugin.getPlayerDataRepository().getPlayerData(playerRef.getUuid());
            return playerData != null ? playerData.getFactionId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public List<SidebarButton> getSidebarButtons(String currentView) {
        List<SidebarButton> buttons = new ArrayList<>();
        boolean isStanding = currentView == null || "standing".equals(currentView);
        boolean isBrackets = "brackets".equals(currentView);
        boolean isShop = "shop".equals(currentView);
        boolean isLeaderboard = "leaderboard".equals(currentView);
        boolean isRanks = "ranks".equals(currentView);
        buttons.add(new SidebarButton("My Standing", "nav:honor:standing", null, "#d4af37", isStanding));
        buttons.add(new SidebarButton("Brackets", "nav:honor:brackets", null, "#d4af37", isBrackets));
        buttons.add(new SidebarButton("Honor Shop", "nav:honor:shop", null, "#d4af37", isShop));
        buttons.add(new SidebarButton("Leaderboard", "nav:honor:leaderboard", null, "#d4af37", isLeaderboard));
        buttons.add(new SidebarButton("All Ranks", "nav:honor:ranks", null, "#d4af37", isRanks));
        return buttons;
    }

    public void renderContent(UICommandBuilder cmd, UIEventBuilder events, String view, String subView) {
        if (!isHonorAvailable()) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "Honor system not available.");
            return;
        }

        Object honorData = getHonorData();
        if (honorData == null) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "Loading honor data...");
            return;
        }

        if (view == null) view = "standing";

        switch (view) {
            case "standing" -> renderStanding(cmd, events, honorData);
            case "brackets" -> renderBrackets(cmd, events);
            case "shop" -> renderShop(cmd, events);
            case "leaderboard" -> renderLeaderboard(cmd, events, subView);
            case "ranks" -> renderRanks(cmd, events);
            default -> renderStanding(cmd, events, honorData);
        }
    }

    private void renderStanding(UICommandBuilder cmd, UIEventBuilder events, Object honorData) {
        Object manager = getHonorManager();
        String factionId = getFactionId();

        Object rank = invokeRank(honorData);
        int rankNumber = invokeRankNumber(rank);
        String rankTitle = invokeTitle(rank, factionId);
        int rankPoints = invokeRankPoints(honorData);
        int weeklyHonor = invokeWeeklyHonor(honorData);
        int weeklyKills = invokeWeeklyKills(honorData);
        int lifetimeKills = invokeLifetimeKills(honorData);
        int spendableHonor = invokeCurrentHonor(honorData);

        int bracket = invokeBracketNumber(manager, weeklyHonor);
        int bracketRP = invokeCalculateBracketRP(manager, weeklyHonor);

        int nextRankRP = getNextRankThreshold(rankNumber);
        int currentRankRP = getCurrentRankThreshold(rankNumber);
        int progressRP = rankPoints - currentRankRP;
        int rangeRP = nextRankRP - currentRankRP;
        float progressPercent = rangeRP > 0 ? (float) progressRP / rangeRP : 1.0f;

        String tierColor = getRankTierColor(rankNumber);

        cmd.set("#HeaderSection.Visible", false);
        cmd.set("#HonorHeroSection.Visible", true);
        cmd.set("#HonorEmblemOuter.Background", tierColor);
        cmd.set("#HonorRankNumber.TextSpans", Message.raw(String.valueOf(rankNumber)).color(Color.decode(tierColor)));
        cmd.set("#HonorRankTitle.TextSpans", Message.raw(rankTitle.toUpperCase()).color(Color.decode(tierColor)));
        cmd.set("#HonorPlayerName.Text", playerRef.getUsername());

        if (factionId != null) {
            var factionsPlugin = HC_FactionsPlugin.getInstance();
            if (factionsPlugin != null) {
                var faction = factionsPlugin.getFactionManager().getFaction(factionId);
                if (faction != null) {
                    cmd.set("#HonorFactionTag.TextSpans", Message.raw("[" + faction.getShortName() + "]").color(Color.decode(faction.getColorHex())));
                }
            }
        }

        cmd.set("#HonorHeroProgressBar.Background", tierColor);
        int heroProgressWidth = (int) (480 * progressPercent);
        Anchor heroProgressAnchor = new Anchor();
        heroProgressAnchor.setWidth(Value.of(heroProgressWidth));
        heroProgressAnchor.setHeight(Value.of(20));
        cmd.setObject("#HonorHeroProgressBar.Anchor", heroProgressAnchor);

        Object nextRank = getNextRank(rankNumber);
        if (nextRank != null) {
            cmd.set("#HonorRPToNextRank.Text", String.format("%,d / %,d RP to %s",
                rankPoints, nextRankRP, invokeTitle(nextRank, factionId)));
        } else {
            cmd.set("#HonorRPToNextRank.Text", String.format("%,d RP -- MAX RANK", rankPoints));
        }

        cmd.set("#HonorWeeklyCard.Visible", true);
        cmd.set("#HonorWeeklyCardSpacer.Visible", true);
        cmd.set("#HonorResetTimer.Text", getTimeUntilWeeklyReset());
        cmd.set("#HonorBracketValue.TextSpans", Message.raw(bracket > 0 ? String.valueOf(bracket) : "-").color(Color.decode(bracket > 0 ? "#4aff7f" : "#555555")));
        cmd.set("#HonorBracketRP.Text", bracket > 0 ? String.format("+%,d RP", bracketRP) : "+0 RP");
        cmd.set("#HonorWeeklyValue.Text", String.format("%,d", weeklyHonor));
        cmd.set("#HonorWeeklyKills.Text", String.valueOf(weeklyKills));

        if (weeklyKills > 0) {
            int avgPerKill = weeklyHonor / weeklyKills;
            cmd.set("#HonorAvgPerKill.Text", String.valueOf(avgPerKill));
        } else {
            cmd.set("#HonorAvgPerKill.Text", "-");
        }

        int honorToNext = invokeGetHonorToNextBracket(manager, weeklyHonor);
        int bracketProgressWidth = 0;
        if (bracket > 1 && honorToNext > 0) {
            cmd.set("#HonorBracketTarget.Text", "Next: Bracket " + (bracket - 1));
            int nextBracketRP = getNextBracketRP(bracket);
            cmd.set("#HonorBracketReward.Text", String.format("+%,d RP", nextBracketRP));
            float bracketProgress = calculateBracketProgress(weeklyHonor, bracket);
            bracketProgressWidth = (int) (130 * bracketProgress);
        } else if (bracket == 1) {
            cmd.set("#HonorBracketTarget.Text", "Top bracket!");
            cmd.set("#HonorBracketReward.Text", String.format("+%,d RP", bracketRP));
            bracketProgressWidth = 130;
        } else {
            cmd.set("#HonorBracketTarget.Text", "Need 1,000+");
            cmd.set("#HonorBracketReward.Text", "+1,500 RP");
            bracketProgressWidth = 0;
        }
        Anchor bracketProgressAnchor = new Anchor();
        bracketProgressAnchor.setWidth(Value.of(bracketProgressWidth));
        bracketProgressAnchor.setHeight(Value.of(12));
        cmd.setObject("#HonorBracketProgressBar.Anchor", bracketProgressAnchor);

        cmd.set("#HonorCurrencyProgressRow.Visible", true);
        cmd.set("#HonorCurrencyProgressSpacer.Visible", true);
        cmd.set("#HonorSpendableValue.Text", String.format("%,d", spendableHonor));

        if (nextRank != null) {
            cmd.set("#HonorNextRankTitle.TextSpans", Message.raw(invokeTitle(nextRank, factionId)).color(Color.decode(getRankTierColor(invokeRankNumber(nextRank)))));
            int rpNeeded = nextRankRP - rankPoints;
            cmd.set("#HonorRPNeeded.Text", String.format("%,d RP needed", rpNeeded));
        } else {
            cmd.set("#HonorNextRankTitle.TextSpans", Message.raw("MAX RANK").color(Color.decode("#ffd700")));
            cmd.set("#HonorRPNeeded.Text", "You've reached the top!");
        }

        cmd.set("#HonorAccountDecayRow.Visible", true);
        cmd.set("#HonorLifetimeKills.Text", String.format("%,d", lifetimeKills));
        cmd.set("#HonorHighestRank.Text", "R" + rankNumber);
        int estimatedDecay = (int) (rankPoints * 0.20);
        cmd.set("#HonorDecayAmount.Text", String.format("-%,d RP", estimatedDecay));
        cmd.set("#FooterText.Text", "Weekly reset: Sunday 00:00 UTC");
    }

    private int getNextBracketRP(int currentBracket) {
        int[][] brackets = {
            {15000, 13000}, {12000, 11000}, {10000, 9000}, {8000, 7000},
            {6000, 5500}, {4000, 4000}, {3000, 3000}, {2000, 2500},
            {1500, 2000}, {1000, 1500}
        };
        int nextBracket = currentBracket - 1;
        if (nextBracket >= 1 && nextBracket <= brackets.length) {
            return brackets[nextBracket - 1][1];
        }
        return 0;
    }

    private float calculateBracketProgress(int weeklyHonor, int currentBracket) {
        int[][] brackets = {
            {15000, 13000}, {12000, 11000}, {10000, 9000}, {8000, 7000},
            {6000, 5500}, {4000, 4000}, {3000, 3000}, {2000, 2500},
            {1500, 2000}, {1000, 1500}
        };
        if (currentBracket <= 0 || currentBracket > brackets.length) return 0.0f;
        int currentThreshold = brackets[currentBracket - 1][0];
        int nextThreshold = currentBracket > 1 ? brackets[currentBracket - 2][0] : currentThreshold + 1000;
        float range = nextThreshold - currentThreshold;
        float progress = weeklyHonor - currentThreshold;
        return Math.min(1.0f, Math.max(0.0f, progress / range));
    }

    private void renderBrackets(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.set("#HeaderSection.Visible", false);

        Object honorData = getHonorData();
        Object manager = getHonorManager();
        int playerBracket = 0;
        int playerWeeklyHonor = 0;
        if (honorData != null && manager != null) {
            playerWeeklyHonor = invokeWeeklyHonor(honorData);
            playerBracket = invokeBracketNumber(manager, playerWeeklyHonor);
        }

        cmd.set("#HonorBracketsSection.Visible", true);

        if (playerBracket > 0) {
            cmd.set("#HonorBracketsTitle.Text", String.format(
                "BRACKETS -- Your weekly: %,d -- Bracket %d (+%,d RP)",
                playerWeeklyHonor, playerBracket, getBracketRP(playerBracket)));
        } else {
            cmd.set("#HonorBracketsTitle.Text", String.format(
                "BRACKETS -- Your weekly: %,d -- No bracket (need 1,000+)",
                playerWeeklyHonor));
        }

        int[][] brackets = {
            {15000, 13000}, {12000, 11000}, {10000, 9000}, {8000, 7000},
            {6000, 5500}, {4000, 4000}, {3000, 3000}, {2000, 2500},
            {1500, 2000}, {1000, 1500},
        };

        for (int i = 0; i < 10; i++) {
            int bracketNum = i + 1;
            boolean isCurrentBracket = (bracketNum == playerBracket);
            cmd.set("#BracketRow" + i + ".Visible", true);
            String numColor = bracketNum <= 3 ? "#ffd700" : "#c0c0c0";
            if (isCurrentBracket) numColor = "#4aff7f";
            cmd.set("#BracketNum" + i + ".TextSpans", Message.raw(String.valueOf(bracketNum)).color(Color.decode(numColor)));
            cmd.set("#BracketReq" + i + ".Text", String.format("%,d+ honor", brackets[i][0]));
            cmd.set("#BracketRP" + i + ".Text", String.format("+%,d RP", brackets[i][1]));
            if (isCurrentBracket) {
                cmd.set("#BracketYou" + i + ".Text", "< YOU");
                cmd.set("#BracketRow" + i + ".Background", "#1e3a2a");
            } else {
                cmd.set("#BracketYou" + i + ".Text", "");
            }
        }
        cmd.set("#FooterText.Text", "Brackets reset every Sunday at midnight UTC. Earn honor by defeating enemy players.");
    }

    private int getBracketRP(int bracketNum) {
        int[][] brackets = {
            {15000, 13000}, {12000, 11000}, {10000, 9000}, {8000, 7000},
            {6000, 5500}, {4000, 4000}, {3000, 3000}, {2000, 2500},
            {1500, 2000}, {1000, 1500}
        };
        if (bracketNum >= 1 && bracketNum <= brackets.length) {
            return brackets[bracketNum - 1][1];
        }
        return 0;
    }

    private void renderShop(UICommandBuilder cmd, UIEventBuilder events) {
        cmd.set("#HonorShopSection.Visible", false);
        cmd.set("#HonorShopComingSoon.Visible", true);
        cmd.set("#FooterText.Text", "The Honor Shop will be available in a future update.");
    }

    @SuppressWarnings("unchecked")
    private void renderLeaderboard(UICommandBuilder cmd, UIEventBuilder events, String type) {
        Object manager = getHonorManager();
        if (manager == null) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "Leaderboard not available.");
            return;
        }

        List<?> leaderboard;
        String title;
        String valueHeader;

        try {
            if ("rp".equals(type)) {
                leaderboard = (List<?>) getRankPointsLeaderboardMethod.invoke(manager, 10);
                title = "RANK POINTS LEADERBOARD";
                valueHeader = "RP";
            } else if ("lifetime".equals(type)) {
                leaderboard = (List<?>) getLifetimeLeaderboardMethod.invoke(manager, 10);
                title = "LIFETIME KILLS LEADERBOARD";
                valueHeader = "KILLS";
            } else {
                leaderboard = (List<?>) getWeeklyLeaderboardMethod.invoke(manager, 10);
                title = "WEEKLY HONOR LEADERBOARD";
                valueHeader = "HONOR";
            }
        } catch (Exception e) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "Leaderboard not available.");
            return;
        }

        cmd.set("#HonorLeaderboardSection.Visible", true);
        cmd.set("#HonorLeaderboardTitle.Text", title);
        cmd.set("#LeaderboardValueHeader.Text", valueHeader);

        int index = 0;
        for (Object entry : leaderboard) {
            if (index >= 10) break;
            String name = invokePlayerName(entry);
            int value;
            if ("rp".equals(type)) {
                value = invokeRankPoints(entry);
            } else if ("lifetime".equals(type)) {
                value = invokeLifetimeKills(entry);
            } else {
                value = invokeWeeklyHonor(entry);
            }
            cmd.set("#LeaderRow" + index + ".Visible", true);
            cmd.set("#LeaderRank" + index + ".Text", "#" + (index + 1));
            cmd.set("#LeaderName" + index + ".Text", name);
            if (name.equals(playerRef.getUsername())) {
                cmd.set("#LeaderName" + index + ".TextSpans", Message.raw(name).color(Color.decode("#4aff7f")));
                cmd.set("#LeaderRow" + index + ".Background", "#1e3a2a");
            }
            cmd.set("#LeaderValue" + index + ".Text", String.format("%,d", value));
            index++;
        }

        for (int i = index; i < 10; i++) {
            cmd.set("#LeaderRow" + i + ".Visible", false);
        }
        cmd.set("#FooterText.Text", "Top players are ranked by their " + valueHeader.toLowerCase() + ".");
    }

    private void renderRanks(UICommandBuilder cmd, UIEventBuilder events) {
        Object honorData = getHonorData();
        String factionId = getFactionId();
        int playerRankNumber = 0;
        if (honorData != null) {
            Object rank = invokeRank(honorData);
            playerRankNumber = invokeRankNumber(rank);
        }

        cmd.set("#HonorRanksSection.Visible", true);

        if (honorData != null) {
            Object rank = invokeRank(honorData);
            cmd.set("#HonorRanksTitle.Text", String.format(
                "ALL RANKS -- Your rank: %s (%,d RP)",
                invokeTitle(rank, factionId), invokeRankPoints(honorData)));
        } else {
            cmd.set("#HonorRanksTitle.Text", "ALL RANKS");
        }

        Object[][] ranks = {
            {1, "Private", 2000},
            {2, "Corporal", 5000},
            {3, "Sergeant", 10000},
            {4, "Master Sergeant", 15000},
            {5, "Sergeant Major", 20000},
            {6, "Knight", 25000},
            {7, "Knight-Lieutenant", 30000},
            {8, "Knight-Captain", 35000},
            {9, "Knight-Champion", 40000},
            {10, "Lieutenant Commander", 45000},
            {11, "Commander", 50000},
            {12, "Marshal", 55000},
            {13, "Field Marshal", 60000},
            {14, "Grand Marshal", 65000}
        };

        for (int i = 0; i < ranks.length && i < 14; i++) {
            int rankNum = (int) ranks[i][0];
            String title = (String) ranks[i][1];
            int rpRequired = (int) ranks[i][2];
            boolean isCurrentRank = (rankNum == playerRankNumber);
            cmd.set("#RankRow" + i + ".Visible", true);
            cmd.set("#RankNum" + i + ".Text", String.valueOf(rankNum));
            cmd.set("#RankTitle" + i + ".Text", title);
            cmd.set("#RankRP" + i + ".Text", String.format("%,d RP", rpRequired));
            if (isCurrentRank) {
                cmd.set("#RankYou" + i + ".Text", "< YOU");
                cmd.set("#RankRow" + i + ".Background", "#1e3a2a");
                cmd.set("#RankTitle" + i + ".TextSpans", Message.raw(title).color(Color.decode("#4aff7f")));
            } else {
                cmd.set("#RankYou" + i + ".Text", "");
            }
        }

        for (int i = ranks.length; i < 14; i++) {
            cmd.set("#RankRow" + i + ".Visible", false);
        }
        cmd.set("#FooterText.Text", "Higher ranks unlock special titles and rewards. 20% RP decay weekly.");
    }

    private String getRankTierColor(int rankNumber) {
        if (rankNumber == 0) return "#808080";
        if (rankNumber <= 3) return "#c0c0c0";
        if (rankNumber <= 5) return "#cd7f32";
        if (rankNumber <= 9) return "#4169e1";
        if (rankNumber <= 12) return "#9932cc";
        return "#ffd700";
    }

    private String getTimeUntilWeeklyReset() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime nextSunday = now.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
            .withHour(0).withMinute(0).withSecond(0).withNano(0);
        Duration duration = Duration.between(now, nextSunday);
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        if (days > 0) return String.format("%dd %dh %dm", days, hours, minutes);
        else if (hours > 0) return String.format("%dh %dm", hours, minutes);
        else return String.format("%dm", minutes);
    }

    private int getNextRankThreshold(int currentRank) {
        int[] thresholds = {0, 2000, 5000, 10000, 15000, 20000, 25000, 30000, 35000, 40000, 45000, 50000, 55000, 60000};
        int nextRank = currentRank + 1;
        if (nextRank < thresholds.length) return thresholds[nextRank];
        return thresholds[thresholds.length - 1];
    }

    private int getCurrentRankThreshold(int currentRank) {
        int[] thresholds = {0, 2000, 5000, 10000, 15000, 20000, 25000, 30000, 35000, 40000, 45000, 50000, 55000, 60000};
        if (currentRank < thresholds.length) return thresholds[currentRank];
        return thresholds[thresholds.length - 1];
    }

    private Object getNextRank(int currentRankNumber) {
        try {
            Object[] allRanks = (Object[]) valuesMethod.invoke(null);
            for (Object rank : allRanks) {
                if (invokeRankNumber(rank) == currentRankNumber + 1) {
                    return rank;
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    public boolean handlePurchase(PlayerRef playerRef, String itemIndex) {
        return false;
    }
}
