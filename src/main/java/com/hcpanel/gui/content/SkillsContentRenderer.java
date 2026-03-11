package com.hcpanel.gui.content;

import com.hcpanel.gui.UnifiedPanelGui.SidebarButton;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

// Direct imports from HC_Professions (optional compileOnly dependency)
import com.hcprofessions.HC_ProfessionsPlugin;
import com.hcprofessions.config.XPCurve;
import com.hcprofessions.managers.AllProfessionManager;
import com.hcprofessions.managers.CraftingGateManager;
import com.hcprofessions.managers.ProfessionManager;
import com.hcprofessions.managers.TradeskillManager;
import com.hcprofessions.models.PlayerAllProfessionData;
import com.hcprofessions.models.PlayerTradeskillData;
import com.hcprofessions.models.Profession;
import com.hcprofessions.models.RecipeGate;
import com.hcprofessions.models.Tradeskill;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Renders Skills module content (Professions and Tradeskills) into the unified panel.
 */
public class SkillsContentRenderer {

    private static final int MAX_BAR_WIDTH = 200;

    private final PlayerRef playerRef;

    public SkillsContentRenderer(PlayerRef playerRef) {
        this.playerRef = playerRef;
    }

    private boolean isProfessionsAvailable() {
        return HC_ProfessionsPlugin.getInstance() != null;
    }

    /**
     * Returns sidebar buttons for the Skills module.
     */
    public List<SidebarButton> getSidebarButtons(String currentView) {
        List<SidebarButton> buttons = new ArrayList<>();

        boolean isProfessions = currentView == null || "professions".equals(currentView);
        boolean isTradeskills = "tradeskills".equals(currentView);

        buttons.add(new SidebarButton("Professions", "nav:skills:professions", null, "#4ecdc4", isProfessions));
        buttons.add(new SidebarButton("Tradeskills", "nav:skills:tradeskills", null, "#4ecdc4", isTradeskills));

        return buttons;
    }

    /**
     * Renders content for the specified view.
     */
    public void renderContent(UICommandBuilder cmd, UIEventBuilder events, String view) {
        if (!isProfessionsAvailable()) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "Professions system not available.");
            return;
        }

        if (view == null) view = "professions";

        switch (view) {
            case "professions" -> renderProfessions(cmd, events);
            case "tradeskills" -> renderTradeskills(cmd, events);
            default -> renderProfessions(cmd, events);
        }
    }

    private void renderProfessions(UICommandBuilder cmd, UIEventBuilder events) {
        HC_ProfessionsPlugin profPlugin = HC_ProfessionsPlugin.getInstance();
        if (profPlugin == null) return;

        UUID uuid = playerRef.getUuid();
        ProfessionManager profManager = profPlugin.getProfessionManager();
        AllProfessionManager allProfManager = profPlugin.getAllProfessionManager();
        Profession mainProfession = profManager.getProfession(uuid);
        int cap = profPlugin.getActionXpService().getNonNativeCraftLevelCap();
        int maxLevel = profManager.getEffectiveLevelCap();

        Map<Profession, PlayerAllProfessionData> allProfData = allProfManager.getPlayerData(uuid);

        // Get recipe gates for milestone tracking
        Collection<RecipeGate> allGates = profPlugin.getCraftingGateManager().getAllGates();

        // Hide header section (not needed for skills view)
        cmd.set("#HeaderSection.Visible", false);

        // Show the skills section
        cmd.set("#SkillsSection.Visible", true);

        // Only show enabled professions
        List<Profession> enabledProfessions = Profession.getEnabledProfessions();

        // Count total recipes unlocked for subtitle
        int totalUnlocked = 0;
        int totalGated = allGates.size();
        RecipeGate nextMainMilestone = null;
        int mainLevel = 0;

        // Section header
        if (mainProfession != null) {
            mainLevel = allProfData.containsKey(mainProfession) ? allProfData.get(mainProfession).getLevel() : 0;
            // Count unlocked recipes across all professions
            for (RecipeGate gate : allGates) {
                if (!gate.enabled()) continue;
                Profession gateProfession = gate.requiredProfession();
                PlayerAllProfessionData data = allProfData.get(gateProfession);
                int playerLevel = data != null ? data.getLevel() : 0;
                if (playerLevel >= gate.requiredLevel()) totalUnlocked++;
            }
            // Find next milestone for main profession
            nextMainMilestone = findNextMilestone(allGates, mainProfession, mainLevel);

            String description = mainProfession.getDescription();
            String subtitle = "Main: " + mainProfession.getDisplayName()
                + (description != null && !description.isEmpty() ? " -- " + description : "")
                + " | Cap: Lv. " + cap + " | Recipes: " + totalUnlocked + "/" + totalGated;
            cmd.set("#SkillsSectionTitle.TextSpans",
                Message.raw("PROFESSIONS (Max Lv. " + maxLevel + ")").color(Color.decode("#4ecdc4")));
            cmd.set("#SkillsSectionSubtitle.Text", subtitle);
        } else {
            cmd.set("#SkillsSectionTitle.TextSpans",
                Message.raw("PROFESSIONS (Max Lv. " + maxLevel + ")").color(Color.decode("#4ecdc4")));
            cmd.set("#SkillsSectionSubtitle.Text", "No profession chosen. Use /profession choose");
        }

        int i = 0;
        for (Profession prof : enabledProfessions) {
            PlayerAllProfessionData data = allProfData.get(prof);
            int level = data != null ? data.getLevel() : 0;
            long currentXp = data != null ? data.getCurrentXp() : 0;
            boolean isMain = prof == mainProfession;
            int effectiveMaxLevel = isMain ? maxLevel : cap;
            boolean isMaxed = level >= effectiveMaxLevel;
            long xpNeeded = XPCurve.getXpToNextLevel(level);

            cmd.set("#SkillRow" + i + ".Visible", true);

            // Skill name with profession color
            cmd.set("#SkillName" + i + ".TextSpans",
                Message.raw(prof.getDisplayName()).color(prof.getColor()));

            // Level
            cmd.set("#SkillLevel" + i + ".TextSpans",
                Message.raw("Lv. " + level).color(Color.WHITE));

            // XP bar fill width (proportional)
            float progress = isMaxed ? 1.0f : (xpNeeded > 0 ? (float) currentXp / xpNeeded : 0);
            int barWidth = Math.max(1, (int) (MAX_BAR_WIDTH * progress));
            Anchor barAnchor = new Anchor();
            barAnchor.setWidth(Value.of(barWidth));
            barAnchor.setHeight(Value.of(8));
            cmd.setObject("#SkillBarFill" + i + ".Anchor", barAnchor);

            // Bar color matches profession color
            cmd.set("#SkillBarFill" + i + ".Background", colorToHex(prof.getColor()));

            // XP text — include total XP earned for context
            long totalXpEarned = data != null ? data.getTotalXpEarned() : 0;
            if (isMaxed) {
                int recipesAtThisLevel = countRecipesForProfession(allGates, prof, level);
                String maxText = totalXpEarned > 0
                    ? String.format("MAX (%d recipes, %,d total XP)", recipesAtThisLevel, totalXpEarned)
                    : "MAX (" + recipesAtThisLevel + " recipes)";
                cmd.set("#SkillXp" + i + ".Text", maxText);
            } else if (level == 0 && currentXp == 0) {
                // Show description for un-started professions
                String desc = prof.getDescription();
                cmd.set("#SkillXp" + i + ".Text", desc != null && !desc.isEmpty() ? desc : "Craft items to earn XP");
            } else {
                String xpText = String.format("%,d / %,d XP", currentXp, xpNeeded);
                if (totalXpEarned > 0) {
                    xpText += String.format(" (%,d total)", totalXpEarned);
                }
                cmd.set("#SkillXp" + i + ".Text", xpText);
            }

            // Badge — show recipe count for professions with unlocked recipes
            int unlockedForProf = countRecipesForProfession(allGates, prof, level);
            int totalForProf = countTotalRecipesForProfession(allGates, prof);

            if (isMain) {
                if (totalForProf > 0) {
                    cmd.set("#SkillBadge" + i + ".TextSpans",
                        Message.raw(unlockedForProf + "/" + totalForProf).color(Color.decode("#4aff7f")));
                } else {
                    cmd.set("#SkillBadge" + i + ".TextSpans",
                        Message.raw("MAIN").color(Color.decode("#4aff7f")));
                }
                cmd.set("#SkillBadge" + i + ".Visible", true);
            } else if (level >= cap) {
                cmd.set("#SkillBadge" + i + ".TextSpans",
                    Message.raw("CAP").color(Color.decode("#e74c3c")));
                cmd.set("#SkillBadge" + i + ".Visible", true);
            } else if (totalForProf > 0 && unlockedForProf > 0) {
                cmd.set("#SkillBadge" + i + ".TextSpans",
                    Message.raw(unlockedForProf + "/" + totalForProf).color(Color.decode("#8fa4b8")));
                cmd.set("#SkillBadge" + i + ".Visible", true);
            } else {
                cmd.set("#SkillBadge" + i + ".Visible", false);
            }

            i++;
        }

        // Hide remaining rows
        for (int j = i; j < 9; j++) {
            cmd.set("#SkillRow" + j + ".Visible", false);
        }

        // Footer with next milestone context
        if (mainProfession != null && nextMainMilestone != null) {
            String recipeName = formatRecipeName(nextMainMilestone.recipeOutputId());
            cmd.set("#FooterText.Text", String.format(
                "Next unlock: %s at Lv. %d (%s). Craft items at workbenches to earn XP.",
                recipeName, nextMainMilestone.requiredLevel(), mainProfession.getDisplayName()));
        } else if (mainProfession != null) {
            cmd.set("#FooterText.Text", "All recipes unlocked for " + mainProfession.getDisplayName() + "! Craft items at workbenches to earn XP.");
        } else {
            cmd.set("#FooterText.Text", "Use /profession choose to set your main. Craft items at workbenches to earn XP.");
        }
    }

    private RecipeGate findNextMilestone(Collection<RecipeGate> gates, Profession prof, int currentLevel) {
        return gates.stream()
            .filter(g -> g.enabled() && g.requiredProfession() == prof && g.requiredLevel() > currentLevel)
            .min(Comparator.comparingInt(RecipeGate::requiredLevel))
            .orElse(null);
    }

    private int countRecipesForProfession(Collection<RecipeGate> gates, Profession prof, int currentLevel) {
        return (int) gates.stream()
            .filter(g -> g.enabled() && g.requiredProfession() == prof && g.requiredLevel() <= currentLevel)
            .count();
    }

    private int countTotalRecipesForProfession(Collection<RecipeGate> gates, Profession prof) {
        return (int) gates.stream()
            .filter(g -> g.enabled() && g.requiredProfession() == prof)
            .count();
    }

    private String formatRecipeName(String recipeOutputId) {
        if (recipeOutputId == null) return "Unknown";
        // Convert "iron_longsword" to "Iron Longsword"
        String[] parts = recipeOutputId.replace("hytale:", "").split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private void renderTradeskills(UICommandBuilder cmd, UIEventBuilder events) {
        HC_ProfessionsPlugin profPlugin = HC_ProfessionsPlugin.getInstance();
        if (profPlugin == null) return;

        UUID uuid = playerRef.getUuid();
        TradeskillManager tsManager = profPlugin.getTradeskillManager();
        int maxLevel = XPCurve.getMaxLevel();

        Map<Tradeskill, PlayerTradeskillData> tsData = tsManager.getPlayerData(uuid);

        // Hide header section (not needed for skills view)
        cmd.set("#HeaderSection.Visible", false);

        // Show the skills section
        cmd.set("#SkillsSection.Visible", true);

        // Count maxed tradeskills for subtitle
        int tsMaxed = 0;
        int tsTotal = 0;
        for (Tradeskill ts : Tradeskill.getEnabledTradeskills()) {
            tsTotal++;
            PlayerTradeskillData d = tsData.get(ts);
            if (d != null && d.getLevel() >= maxLevel) tsMaxed++;
        }

        // Section header
        cmd.set("#SkillsSectionTitle.TextSpans",
            Message.raw("TRADESKILLS (Max Lv. " + maxLevel + ")").color(Color.decode("#4ecdc4")));
        String tsSubtitle = "Gathering skills -- higher levels unlock resources and increase yield";
        if (tsMaxed > 0) {
            tsSubtitle += " | Mastered: " + tsMaxed + "/" + tsTotal;
        }
        cmd.set("#SkillsSectionSubtitle.Text", tsSubtitle);

        int i = 0;
        for (Tradeskill ts : Tradeskill.getEnabledTradeskills()) {
            PlayerTradeskillData data = tsData.get(ts);
            int level = data != null ? data.getLevel() : 0;
            long currentXp = data != null ? data.getCurrentXp() : 0;
            boolean isMaxed = level >= maxLevel;
            long xpNeeded = XPCurve.getXpToNextLevel(level);

            cmd.set("#SkillRow" + i + ".Visible", true);

            // Skill name with tradeskill color
            cmd.set("#SkillName" + i + ".TextSpans",
                Message.raw(ts.getDisplayName()).color(ts.getColor()));

            // Level
            cmd.set("#SkillLevel" + i + ".TextSpans",
                Message.raw("Lv. " + level).color(Color.WHITE));

            // XP bar fill width
            float progress = isMaxed ? 1.0f : (xpNeeded > 0 ? (float) currentXp / xpNeeded : 0);
            int barWidth = Math.max(1, (int) (MAX_BAR_WIDTH * progress));
            Anchor barAnchor = new Anchor();
            barAnchor.setWidth(Value.of(barWidth));
            barAnchor.setHeight(Value.of(8));
            cmd.setObject("#SkillBarFill" + i + ".Anchor", barAnchor);

            // Bar color matches tradeskill color
            cmd.set("#SkillBarFill" + i + ".Background", colorToHex(ts.getColor()));

            // XP text with total earned for context
            long tsTotalXp = data != null ? data.getTotalXpEarned() : 0;
            String xpText;
            if (isMaxed) {
                xpText = tsTotalXp > 0 ? String.format("MAX (%,d total XP)", tsTotalXp) : "MAX";
            } else if (level == 0 && currentXp == 0) {
                // Show description for un-started tradeskills
                xpText = getTradeskillDescription(ts);
            } else {
                xpText = String.format("%,d / %,d XP", currentXp, xpNeeded);
                if (tsTotalXp > 0) xpText += String.format(" (%,d total)", tsTotalXp);
            }
            cmd.set("#SkillXp" + i + ".Text", xpText);

            // Show bonus yield chance as badge
            if (level > 0) {
                cmd.set("#SkillBadge" + i + ".TextSpans",
                    Message.raw("+" + level + "%").color(Color.decode("#f39c12")));
                cmd.set("#SkillBadge" + i + ".Visible", true);
            } else {
                cmd.set("#SkillBadge" + i + ".Visible", false);
            }

            i++;
        }

        // Hide remaining rows (6 tradeskills, hide rows 6-8)
        for (int j = i; j < 9; j++) {
            cmd.set("#SkillRow" + j + ".Visible", false);
        }

        // Calculate total yield bonus across all tradeskills
        int totalLevels = 0;
        for (Tradeskill ts : Tradeskill.getEnabledTradeskills()) {
            PlayerTradeskillData data2 = tsData.get(ts);
            if (data2 != null) totalLevels += data2.getLevel();
        }
        cmd.set("#FooterText.Text", "Each level grants +1% bonus yield chance. Badge shows your yield bonus. Total combined: +" + totalLevels + "% across all tradeskills.");
    }

    private String getTradeskillDescription(Tradeskill ts) {
        return switch (ts) {
            case MINING -> "Harvest ore and stone from rocks";
            case WOODCUTTING -> "Chop logs and planks from trees";
            case FARMING -> "Grow crops and produce from soil";
            case HERBALISM -> "Gather herbs and reagents from plants";
            case SKINNING -> "Collect hides and leather from creatures";
            case FISHING -> "Catch fish and aquatic materials";
        };
    }

    private static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
