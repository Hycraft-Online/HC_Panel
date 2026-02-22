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
import com.hcprofessions.managers.ProfessionManager;
import com.hcprofessions.managers.TradeskillManager;
import com.hcprofessions.models.PlayerAllProfessionData;
import com.hcprofessions.models.PlayerTradeskillData;
import com.hcprofessions.models.Profession;
import com.hcprofessions.models.Tradeskill;

import java.awt.Color;
import java.util.ArrayList;
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

        // Hide header section (not needed for skills view)
        cmd.set("#HeaderSection.Visible", false);

        // Show the skills section
        cmd.set("#SkillsSection.Visible", true);

        // Only show enabled professions
        List<Profession> enabledProfessions = Profession.getEnabledProfessions();

        // Section header
        if (mainProfession != null) {
            cmd.set("#SkillsSectionTitle.TextSpans",
                Message.raw("PROFESSIONS (Max Lv. " + maxLevel + ")").color(Color.decode("#4ecdc4")));
            cmd.set("#SkillsSectionSubtitle.Text",
                "Main: " + mainProfession.getDisplayName() + " | Non-main cap: Lv. " + cap);
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

            // XP text
            String xpText = isMaxed ? "MAX" : String.format("%,d / %,d XP", currentXp, xpNeeded);
            cmd.set("#SkillXp" + i + ".Text", xpText);

            // Badge (use .TextSpans to avoid .Style.TextColor corruption)
            if (isMain) {
                cmd.set("#SkillBadge" + i + ".TextSpans",
                    Message.raw("MAIN").color(Color.decode("#4aff7f")));
                cmd.set("#SkillBadge" + i + ".Visible", true);
            } else if (level >= cap) {
                cmd.set("#SkillBadge" + i + ".TextSpans",
                    Message.raw("CAP").color(Color.decode("#e74c3c")));
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

        cmd.set("#FooterText.Text", "Craft items at benches to earn XP. Higher levels unlock advanced recipes.");
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

        // Section header
        cmd.set("#SkillsSectionTitle.TextSpans",
            Message.raw("TRADESKILLS (Max Lv. " + maxLevel + ")").color(Color.decode("#4ecdc4")));
        cmd.set("#SkillsSectionSubtitle.Text", "Gathering skills -- higher levels unlock resources and increase yield");

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

            // XP text
            String xpText = isMaxed ? "MAX" : String.format("%,d / %,d XP", currentXp, xpNeeded);
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

        cmd.set("#FooterText.Text", "Each level grants +1% bonus yield chance when gathering. Level up to unlock higher-tier resources.");
    }

    private static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }
}
