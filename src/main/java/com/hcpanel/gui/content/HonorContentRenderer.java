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
    private static volatile boolean reflectionInitialized = false;
    private static volatile boolean honorAvailable = false;
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
    // Shop methods
    private static Method getShopItemsMethod;
    private static Method purchaseItemMethod;
    // HonorShopItem methods
    private static Method shopGetDisplayNameMethod;
    private static Method shopGetDescriptionMethod;
    private static Method shopGetHonorCostMethod;
    private static Method shopGetRequiredRankMethod;
    private static Method shopGetItemIdMethod;
    private static Method shopCanPurchaseMethod;
    // Cached shop items for purchase handling
    private List<?> cachedShopItems;

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

            // Shop methods
            getShopItemsMethod = managerClass.getMethod("getShopItems");
            Class<?> shopItemClass = Class.forName("com.hchonor.models.HonorShopItem");
            purchaseItemMethod = managerClass.getMethod("purchaseItem", UUID.class, String.class, shopItemClass);
            shopGetDisplayNameMethod = shopItemClass.getMethod("getDisplayName");
            shopGetDescriptionMethod = shopItemClass.getMethod("getDescription");
            shopGetHonorCostMethod = shopItemClass.getMethod("getHonorCost");
            shopGetRequiredRankMethod = shopItemClass.getMethod("getRequiredRank");
            shopGetItemIdMethod = shopItemClass.getMethod("getItemId");
            Class<?> honorDataClass = Class.forName("com.hchonor.models.HonorData");
            shopCanPurchaseMethod = shopItemClass.getMethod("canPurchase", honorDataClass);

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
        boolean isRanks = "ranks".equals(currentView);
        boolean isHowItWorks = "how-it-works".equals(currentView);

        // Leaderboard sub-views — detect which type is active
        boolean isLbWeekly = "leaderboard".equals(currentView);     // default leaderboard = weekly
        boolean isLbRP = "leaderboard-rp".equals(currentView);
        boolean isLbLifetime = "leaderboard-lt".equals(currentView);
        boolean isAnyLb = isLbWeekly || isLbRP || isLbLifetime;

        buttons.add(new SidebarButton("My Standing", "nav:honor:standing", null, "#d4af37", isStanding));
        buttons.add(new SidebarButton("How It Works", "nav:honor:how-it-works", null, "#e8c547", isHowItWorks));
        buttons.add(new SidebarButton("Brackets", "nav:honor:brackets", null, "#d4af37", isBrackets));

        // Leaderboard: 3 sub-types so player can switch between views
        buttons.add(new SidebarButton("LB: Weekly", "nav:honor:leaderboard", null, "#e8c547", isLbWeekly));
        buttons.add(new SidebarButton("LB: Rank Points", "nav:honor:leaderboard-rp", null, "#e8c547", isLbRP));
        buttons.add(new SidebarButton("LB: Lifetime", "nav:honor:leaderboard-lt", null, "#e8c547", isLbLifetime));

        buttons.add(new SidebarButton("All Ranks", "nav:honor:ranks", null, "#d4af37", isRanks));
        buttons.add(new SidebarButton("Honor Shop", "nav:honor:shop", null, "#d4af37", isShop));
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
            case "how-it-works" -> renderHowItWorks(cmd, events, honorData);
            case "brackets" -> renderBrackets(cmd, events);
            case "shop" -> renderShop(cmd, events);
            case "leaderboard" -> renderLeaderboard(cmd, events, null);        // Weekly (default)
            case "leaderboard-rp" -> renderLeaderboard(cmd, events, "rp");     // Rank Points
            case "leaderboard-lt" -> renderLeaderboard(cmd, events, "lifetime"); // Lifetime Kills
            case "ranks" -> renderRanks(cmd, events);
            default -> renderStanding(cmd, events, honorData);
        }
    }

    private void renderHowItWorks(UICommandBuilder cmd, UIEventBuilder events, Object honorData) {
        cmd.set("#HeaderSection.Visible", false);
        cmd.set("#ContentSpacer.Visible", false);
        cmd.set("#GuideSection.Visible", true);

        // Reset guide elements
        cmd.set("#GuideHeaderImage.Visible", false);
        cmd.set("#GuideHeaderSpacer.Visible", false);
        cmd.set("#GuideIntroCard.Visible", false);
        cmd.set("#GuideIntroSpacer.Visible", false);
        for (int i = 0; i < 6; i++) {
            cmd.set("#GuideCard" + i + ".Visible", false);
            cmd.set("#GuideCard" + i + "Spacer.Visible", false);
            cmd.set("#GuideCard" + i + "Bullets.Visible", false);
            for (int j = 0; j < 6; j++) {
                cmd.set("#GuideCard" + i + "Bullet" + j + ".Visible", false);
            }
        }
        cmd.set("#GuideStatsRow.Visible", false);
        cmd.set("#GuideStatsRowSpacer.Visible", false);
        for (int i = 0; i < 4; i++) cmd.set("#GuideStat" + i + ".Visible", false);
        cmd.set("#GuideTipCard.Visible", false);

        // Intro
        cmd.set("#GuideIntroCard.Visible", true);
        cmd.set("#GuideIntroSpacer.Visible", true);
        cmd.set("#GuideIntroText.Text",
            "The Honor system rewards PvP combat with rank progression and exclusive rewards. "
            + "Earn honor by defeating enemy-faction players, climb brackets for weekly RP payouts, "
            + "and advance through 14 military ranks. Here is everything you need to know.");

        // Card 0: Earning Honor
        showHonorCard(cmd, 0, "\u2694", "EARNING HONOR", "#d4af37",
            "Honor is earned by killing players from the enemy faction. "
            + "Each kill awards a base amount of honor, modified by the target's rank and level. "
            + "Higher-ranked targets are worth significantly more.",
            "Kill an enemy player to earn honor (amount varies by target rank)",
            "Repeated kills on the same player yield diminishing returns",
            "Only faction vs. faction kills count -- friendly fire gives no honor",
            "Honor earned this week goes toward your weekly bracket placement");

        // Card 1: Weekly Cycle & Brackets
        showHonorCard(cmd, 1, "\u23F0", "WEEKLY CYCLE & BRACKETS", "#4aff7f",
            "Every week resets on Sunday at 00:00 UTC. Your total weekly honor determines "
            + "which of 10 brackets you fall into. Higher brackets award more Rank Points (RP) "
            + "when the week ends.",
            "Bracket 1 (top): 15,000+ honor = +13,000 RP",
            "Bracket 5: 6,000+ honor = +5,500 RP",
            "Bracket 10 (min): 1,000+ honor = +1,500 RP",
            "Below 1,000 weekly honor = no bracket, no RP payout",
            "Weekly honor resets to 0 every Sunday -- start fresh each week");

        // Card 2: Rank Points & Decay
        showHonorCard(cmd, 2, "\u26A0", "RANK POINTS & DECAY", "#e74c3c",
            "Rank Points (RP) determine your permanent rank. Each week follows this order: "
            + "first, 20% of your current RP decays away, then your bracket RP is added. "
            + "To climb ranks, your bracket payout must outpace the decay.",
            "Decay happens FIRST: you lose 20% of current RP every Sunday",
            "Bracket RP is added AFTER decay -- net gain depends on bracket",
            "Example: 10,000 RP -> -2,000 decay -> +5,500 (B5) = 13,500 RP",
            "To maintain rank: need bracket RP >= 20% of current RP",
            "Break-even at B5 with 27,500 RP; higher ranks need higher brackets");

        // Card 3: Ranks & Titles
        showHonorCard(cmd, 3, "\u2B50", "RANKS & TITLES", "#ffd700",
            "There are 14 ranks from Private (R1) to Grand Marshal (R14). "
            + "Each rank requires a specific RP threshold. Your title is faction-specific -- "
            + "Alliance and Horde have different rank names at each tier.",
            "R1 Private: 2,000 RP | R5 Sergeant Major: 20,000 RP",
            "R8 Knight-Captain: 35,000 RP | R10 Lt. Commander: 45,000 RP",
            "R12 Marshal: 55,000 RP | R14 Grand Marshal: 65,000 RP",
            "Higher ranks unlock exclusive shop items and prestige titles");

        // Card 4: Honor Currency & Shop
        showHonorCard(cmd, 4, "\u2B55", "HONOR CURRENCY & SHOP", "#8b5cf6",
            "In addition to weekly honor (which resets), you accumulate spendable honor "
            + "that persists across weeks. This currency is spent in the Honor Shop on "
            + "exclusive equipment, cosmetics, and other rewards.",
            "Spendable honor never decays -- only spent when you buy items",
            "Some shop items require a minimum rank to purchase",
            "Open the Honor Shop from the sidebar to browse available items",
            "Prices vary -- save up for high-value rewards");

        // Card 5: Strategy Tips
        showHonorCard(cmd, 5, "\u2728", "STRATEGY TIPS", "#4ecdc4",
            "Climbing the ranks takes consistent effort. "
            + "Focus on earning enough weekly honor to stay in the highest bracket you can sustain.",
            "Aim for at least 1,000 honor/week to get a bracket payout",
            "To push ranks: grind hard early in the week to lock a high bracket",
            "Group PvP is more efficient -- coordinate with your faction",
            "Watch the decay math -- pushing too fast without sustaining = rank loss",
            "Check My Standing to see your estimated RP after next reset");

        // Stats row — personalized with player data
        if (honorData != null) {
            Object manager = getHonorManager();
            int rankPoints = invokeRankPoints(honorData);
            int weeklyHonor = invokeWeeklyHonor(honorData);
            int bracket = manager != null ? invokeBracketNumber(manager, weeklyHonor) : 0;
            int estimatedDecay = (int) (rankPoints * 0.20);
            int bracketRP = manager != null ? invokeCalculateBracketRP(manager, weeklyHonor) : 0;
            int netRP = bracketRP - estimatedDecay;

            cmd.set("#GuideStatsRow.Visible", true);
            cmd.set("#GuideStatsRowSpacer.Visible", true);

            cmd.set("#GuideStat0.Visible", true);
            cmd.set("#GuideStat0Label.Text", "YOUR WEEKLY");
            cmd.set("#GuideStat0Value.TextSpans", Message.raw(String.format("%,d", weeklyHonor)).color(Color.decode("#d4af37")));
            cmd.set("#GuideStat0Desc.Text", "honor earned");

            cmd.set("#GuideStat1.Visible", true);
            cmd.set("#GuideStat1Label.Text", "BRACKET");
            cmd.set("#GuideStat1Value.TextSpans", Message.raw(bracket > 0 ? String.valueOf(bracket) : "None").color(Color.decode(bracket > 0 ? "#4aff7f" : "#e74c3c")));
            cmd.set("#GuideStat1Desc.Text", bracket > 0 ? String.format("+%,d RP", bracketRP) : "need 1,000+");

            cmd.set("#GuideStat2.Visible", true);
            cmd.set("#GuideStat2Label.Text", "DECAY");
            cmd.set("#GuideStat2Value.TextSpans", Message.raw(String.format("-%,d", estimatedDecay)).color(Color.decode("#e74c3c")));
            cmd.set("#GuideStat2Desc.Text", "RP this reset");

            cmd.set("#GuideStat3.Visible", true);
            cmd.set("#GuideStat3Label.Text", "NET CHANGE");
            String netColor = netRP >= 0 ? "#4aff7f" : "#e74c3c";
            String netSign = netRP >= 0 ? "+" : "";
            cmd.set("#GuideStat3Value.TextSpans", Message.raw(netSign + String.format("%,d", netRP)).color(Color.decode(netColor)));
            cmd.set("#GuideStat3Desc.Text", "RP after reset");
        }

        // Tip
        cmd.set("#GuideTipCard.Visible", true);
        cmd.set("#GuideTipText.Text", "Tip: Your decay is based on current RP, not rank threshold. "
            + "If you push RP high without sustaining bracket income, decay will pull you back down. "
            + "Consistency matters more than one big week.");

        cmd.set("#FooterText.Text", "Weekly reset: Sunday 00:00 UTC. Check My Standing for personalized stats and projections.");
    }

    private void showHonorCard(UICommandBuilder cmd, int index, String icon, String title,
                               String accentColor, String text, String... bullets) {
        String prefix = "#GuideCard" + index;
        cmd.set(prefix + ".Visible", true);
        cmd.set(prefix + "Spacer.Visible", true);
        cmd.set(prefix + "Icon.Text", icon);
        cmd.set(prefix + "Title.Text", title);
        cmd.set(prefix + "Text.Text", text);
        if (accentColor != null) {
            cmd.set(prefix + "Accent.Background", accentColor);
            cmd.set(prefix + "Icon.TextSpans", Message.raw(icon).color(Color.decode(accentColor)));
        }
        if (bullets != null && bullets.length > 0) {
            cmd.set(prefix + "Bullets.Visible", true);
            for (int i = 0; i < bullets.length && i < 6; i++) {
                cmd.set(prefix + "Bullet" + i + ".Visible", true);
                cmd.set(prefix + "Bullet" + i + "Text.Text", bullets[i]);
            }
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

        // Estimated RP after this week's reset: current - 20% decay + bracket RP
        int estimatedRPAfterReset = rankPoints - estimatedDecay + bracketRP;
        cmd.set("#FooterText.Text", String.format(
            "Weekly reset: Sunday 00:00 UTC | Estimated RP after reset: %,d (-%,d decay +%,d bracket)",
            estimatedRPAfterReset, estimatedDecay, bracketRP));
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

    @SuppressWarnings("unchecked")
    private void renderShop(UICommandBuilder cmd, UIEventBuilder events) {
        Object manager = getHonorManager();
        Object honorData = getHonorData();

        if (manager == null || honorData == null || getShopItemsMethod == null) {
            cmd.set("#HonorShopComingSoon.Visible", true);
            cmd.set("#FooterText.Text", "Honor shop data not available.");
            return;
        }

        List<?> shopItems;
        try {
            shopItems = (List<?>) getShopItemsMethod.invoke(manager);
        } catch (Exception e) {
            cmd.set("#HonorShopComingSoon.Visible", true);
            cmd.set("#FooterText.Text", "Failed to load shop items.");
            return;
        }

        if (shopItems == null || shopItems.isEmpty()) {
            cmd.set("#HonorShopComingSoon.Visible", true);
            cmd.set("#FooterText.Text", "No items currently available in the shop.");
            return;
        }

        // Cache items for purchase handling
        this.cachedShopItems = shopItems;

        cmd.set("#HeaderSection.Visible", false);
        cmd.set("#HonorShopSection.Visible", true);

        // Player balance and rank info
        int spendableHonor = invokeCurrentHonor(honorData);
        Object rank = invokeRank(honorData);
        int rankNumber = invokeRankNumber(rank);
        String factionId = getFactionId();

        cmd.set("#HonorShopBalance.Text", String.format("%,d Honor", spendableHonor));
        cmd.set("#HonorShopRankInfo.Text", "Your Rank: " + rankNumber);

        cmd.set("#HonorShopTitle.TextSpans",
            Message.raw("HONOR SHOP").color(Color.decode("#d4af37")));

        for (int i = 0; i < 6; i++) {
            if (i < shopItems.size()) {
                Object item = shopItems.get(i);
                try {
                    String name = (String) shopGetDisplayNameMethod.invoke(item);
                    String desc = (String) shopGetDescriptionMethod.invoke(item);
                    int cost = (int) shopGetHonorCostMethod.invoke(item);
                    int reqRank = (int) shopGetRequiredRankMethod.invoke(item);
                    boolean canBuy = (boolean) shopCanPurchaseMethod.invoke(item, honorData);

                    cmd.set("#ShopRow" + i + ".Visible", true);
                    cmd.set("#ShopItem" + i + ".Text", name);
                    cmd.set("#ShopDesc" + i + ".Text", desc != null ? desc : "");
                    cmd.set("#ShopCost" + i + ".TextSpans",
                        Message.raw(String.format("%,d", cost)).color(
                            Color.decode(canBuy ? "#d4af37" : "#555555")));

                    if (reqRank > 0) {
                        String rankText = "R" + reqRank + "+";
                        String rankColor = rankNumber >= reqRank ? "#4aff7f" : "#e74c3c";
                        cmd.set("#ShopRank" + i + ".TextSpans",
                            Message.raw(rankText).color(Color.decode(rankColor)));
                    } else {
                        cmd.set("#ShopRank" + i + ".Text", "-");
                    }

                    // Highlight row if affordable
                    if (canBuy) {
                        cmd.set("#ShopRow" + i + ".Background", "#1b2532(0.8)");
                        events.addEventBinding(CustomUIEventBindingType.Activating, "#ShopBuy" + i,
                            EventData.of("Action", "action:buy_honor:" + i), false);
                    } else {
                        cmd.set("#ShopRow" + i + ".Background", "#0f1520(0.6)");
                        cmd.set("#ShopBuy" + i + ".Text", rankNumber < reqRank ? "LOCKED" : "NEED");
                    }
                } catch (Exception e) {
                    cmd.set("#ShopRow" + i + ".Visible", false);
                }
            } else {
                cmd.set("#ShopRow" + i + ".Visible", false);
            }
        }

        cmd.set("#FooterText.Text", "Spend earned honor on exclusive rewards. Honor is earned through PvP kills.");
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
        String factionId = getFactionId();
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

            // For RP leaderboard, show rank title next to name
            String displayName = name;
            if ("rp".equals(type)) {
                Object entryRank = invokeRank(entry);
                if (entryRank != null) {
                    int entryRankNum = invokeRankNumber(entryRank);
                    if (entryRankNum > 0) {
                        String rankTitle = invokeTitle(entryRank, factionId);
                        displayName = name + " [" + rankTitle + "]";
                    }
                }
            } else if (type == null) {
                // Weekly leaderboard - show bracket number
                int entryWeekly = invokeWeeklyHonor(entry);
                int entryBracket = invokeBracketNumber(manager, entryWeekly);
                if (entryBracket > 0) {
                    displayName = name + " [B" + entryBracket + "]";
                }
            }

            cmd.set("#LeaderName" + index + ".Text", displayName);
            if (name.equals(playerRef.getUsername())) {
                cmd.set("#LeaderName" + index + ".TextSpans", Message.raw(displayName).color(Color.decode("#4aff7f")));
                cmd.set("#LeaderRow" + index + ".Background", "#1e3a2a");
            }
            cmd.set("#LeaderValue" + index + ".Text", String.format("%,d", value));
            index++;
        }

        for (int i = index; i < 10; i++) {
            cmd.set("#LeaderRow" + i + ".Visible", false);
        }
        String footerText = switch (type != null ? type : "weekly") {
            case "rp" -> "Rank Points determine your PvP title. 20% decays weekly, bracket RP is added after decay.";
            case "lifetime" -> "Lifetime kills never reset. A measure of total PvP experience.";
            default -> "Weekly honor resets every Sunday at midnight UTC. Higher honor earns better brackets.";
        };
        cmd.set("#FooterText.Text", footerText);
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

        // Use reflection to get all HonorRank values for faction-specific titles
        int[] rpThresholds = {0, 2000, 5000, 10000, 15000, 20000, 25000, 30000, 35000, 40000, 45000, 50000, 55000, 60000, 65000};
        Object[] allRanks = null;
        try {
            allRanks = (Object[]) valuesMethod.invoke(null);
        } catch (Exception ignored) {}

        for (int i = 0; i < 14; i++) {
            int rankNum = i + 1;
            // Get faction-specific title from the HonorRank enum
            String title = "Rank " + rankNum;
            if (allRanks != null) {
                for (Object r : allRanks) {
                    if (invokeRankNumber(r) == rankNum) {
                        title = invokeTitle(r, factionId);
                        break;
                    }
                }
            }
            int rpRequired = rankNum < rpThresholds.length ? rpThresholds[rankNum] : 65000;
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
        int[] thresholds = {0, 2000, 5000, 10000, 15000, 20000, 25000, 30000, 35000, 40000, 45000, 50000, 55000, 60000, 65000};
        int nextRank = currentRank + 1;
        if (nextRank < thresholds.length) return thresholds[nextRank];
        return thresholds[thresholds.length - 1];
    }

    private int getCurrentRankThreshold(int currentRank) {
        int[] thresholds = {0, 2000, 5000, 10000, 15000, 20000, 25000, 30000, 35000, 40000, 45000, 50000, 55000, 60000, 65000};
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
        if (cachedShopItems == null || purchaseItemMethod == null) return false;

        int index;
        try {
            index = Integer.parseInt(itemIndex);
        } catch (NumberFormatException e) {
            return false;
        }

        if (index < 0 || index >= cachedShopItems.size()) return false;

        Object item = cachedShopItems.get(index);
        Object manager = getHonorManager();
        if (manager == null) return false;

        try {
            boolean success = (boolean) purchaseItemMethod.invoke(
                manager, playerRef.getUuid(), playerRef.getUsername(), item);
            if (success) {
                String name = (String) shopGetDisplayNameMethod.invoke(item);
                int cost = (int) shopGetHonorCostMethod.invoke(item);
                playerRef.sendMessage(Message.raw(
                    "Purchased " + name + " for " + String.format("%,d", cost) + " honor!").color(Color.GREEN));
                return true;
            } else {
                playerRef.sendMessage(Message.raw(
                    "Cannot purchase - insufficient honor or rank.").color(Color.RED));
                return false;
            }
        } catch (Exception e) {
            playerRef.sendMessage(Message.raw("Purchase failed.").color(Color.RED));
            return false;
        }
    }
}
