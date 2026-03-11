package com.hcpanel.gui.content;

import com.hcpanel.gui.UnifiedPanelGui.SidebarButton;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders Settings content into the unified panel.
 * Uses dedicated SettingsToggle elements in the GuideSection.
 */
public class SettingsContentRenderer {

    private static final String SETTINGS_COLOR = "#6c7a89";
    private static final int MAX_CARDS = 6;
    private static final int MAX_BULLETS_PER_CARD = 6;

    private final PlayerRef playerRef;

    public SettingsContentRenderer(PlayerRef playerRef) {
        this.playerRef = playerRef;
    }

    public List<SidebarButton> getSidebarButtons(String currentView) {
        List<SidebarButton> buttons = new ArrayList<>();
        boolean isCombat = currentView == null || "combat".equals(currentView);
        boolean isAbout = "about".equals(currentView);
        buttons.add(new SidebarButton("Combat", "nav:settings:combat", null, SETTINGS_COLOR, isCombat));
        buttons.add(new SidebarButton("About", "nav:settings:about", null, SETTINGS_COLOR, isAbout));
        return buttons;
    }

    public void renderContent(UICommandBuilder cmd, UIEventBuilder events, String view) {
        // Hide the header section and content spacer, show the guide section
        cmd.set("#HeaderSection.Visible", false);
        cmd.set("#ContentSpacer.Visible", false);
        cmd.set("#GuideSection.Visible", true);

        // Reset all guide elements
        resetGuideElements(cmd);

        // Default to combat view
        if (view == null) view = "combat";

        switch (view) {
            case "about" -> renderAbout(cmd, events);
            default -> renderCombatSettings(cmd, events);
        }
    }

    private void renderCombatSettings(UICommandBuilder cmd, UIEventBuilder events) {
        boolean sctOn = isSctEnabled();

        String accentColor = sctOn ? "#4aff7f" : "#e74c3c";
        String statusText = sctOn ? "ON" : "OFF";
        String statusColor = sctOn ? "#4aff7f" : "#e74c3c";
        String btnText = sctOn ? "DISABLE" : "ENABLE";

        // Intro card explaining combat settings
        cmd.set("#GuideIntroCard.Visible", true);
        cmd.set("#GuideIntroSpacer.Visible", true);
        cmd.set("#GuideIntroText.Text",
            "Configure how combat information is displayed during gameplay. " +
            "These settings affect visual feedback and do not change game mechanics.");

        // Show the toggle row
        cmd.set("#SettingsToggle0.Visible", true);
        cmd.set("#SettingsToggle0Spacer.Visible", true);
        cmd.set("#SettingsToggle0Accent.Background", accentColor);
        cmd.set("#SettingsToggle0Title.Text", "SCROLLING BATTLE TEXT");
        cmd.set("#SettingsToggle0Status.TextSpans", Message.raw("Status: " + statusText).color(Color.decode(statusColor)));
        cmd.set("#SettingsToggle0Btn.Text", btnText);

        // Bind the toggle button
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SettingsToggle0Btn",
            EventData.of("Action", "action:setting:sbt_toggle"), false);

        // Description card for SCT
        cmd.set("#GuideCard0.Visible", true);
        cmd.set("#GuideCard0Spacer.Visible", true);
        cmd.set("#GuideCard0Title.TextSpans", Message.raw("What is Scrolling Battle Text?").color(Color.decode(SETTINGS_COLOR)));
        cmd.set("#GuideCard0Bullets.Visible", true);
        cmd.set("#GuideCard0Bullet0.Visible", true);
        cmd.set("#GuideCard0Bullet0Text.Text", "Shows floating damage numbers above targets when you deal damage");
        cmd.set("#GuideCard0Bullet1.Visible", true);
        cmd.set("#GuideCard0Bullet1Text.Text", "Shows healing numbers when you receive healing effects");
        cmd.set("#GuideCard0Bullet2.Visible", true);
        cmd.set("#GuideCard0Bullet2Text.Text", "Color-coded: white for physical, blue for magic, green for heals");
        cmd.set("#GuideCard0Bullet3.Visible", true);
        cmd.set("#GuideCard0Bullet3Text.Text", "Disable if the floating text is distracting during combat");

        cmd.set("#FooterText.Text", "Click a button to toggle the setting on or off.");
    }

    private void renderAbout(UICommandBuilder cmd, UIEventBuilder events) {
        // Show about info using guide cards
        cmd.set("#GuideIntroCard.Visible", true);
        cmd.set("#GuideIntroSpacer.Visible", true);
        cmd.set("#GuideIntroText.Text",
            "World of Hycraft is a custom Hytale server featuring faction PvP, "
            + "RPG classes, talent trees, professions, honor-based rankings, and more. "
            + "Open the Guide from the main panel to learn about every system in detail.");

        // Essential commands card
        cmd.set("#GuideCard0.Visible", true);
        cmd.set("#GuideCard0Spacer.Visible", true);
        cmd.set("#GuideCard0Icon.TextSpans", Message.raw("/").color(Color.decode("#4a9eff")));
        cmd.set("#GuideCard0Accent.Background", "#4a9eff");
        cmd.set("#GuideCard0Title.Text", "ESSENTIAL COMMANDS");
        cmd.set("#GuideCard0Text.Text", "Core commands every player should know.");
        cmd.set("#GuideCard0Bullets.Visible", true);
        cmd.set("#GuideCard0Bullet0.Visible", true);
        cmd.set("#GuideCard0Bullet0Text.Text", "/menu — Open this character panel");
        cmd.set("#GuideCard0Bullet1.Visible", true);
        cmd.set("#GuideCard0Bullet1Text.Text", "/faction — Choose or view your faction");
        cmd.set("#GuideCard0Bullet2.Visible", true);
        cmd.set("#GuideCard0Bullet2Text.Text", "/class — Choose your character class");
        cmd.set("#GuideCard0Bullet3.Visible", true);
        cmd.set("#GuideCard0Bullet3Text.Text", "/profession choose — Select your main profession");
        cmd.set("#GuideCard0Bullet4.Visible", true);
        cmd.set("#GuideCard0Bullet4Text.Text", "/party — Create or join a party for group content");
        cmd.set("#GuideCard0Bullet5.Visible", true);
        cmd.set("#GuideCard0Bullet5Text.Text", "/guild — Guild management (create, invite, leave)");

        // Social & economy commands
        cmd.set("#GuideCard1.Visible", true);
        cmd.set("#GuideCard1Spacer.Visible", true);
        cmd.set("#GuideCard1Icon.TextSpans", Message.raw("$").color(Color.decode("#ffd700")));
        cmd.set("#GuideCard1Accent.Background", "#ffd700");
        cmd.set("#GuideCard1Title.Text", "SOCIAL & ECONOMY COMMANDS");
        cmd.set("#GuideCard1Text.Text", "Trading, recruiting, and social features.");
        cmd.set("#GuideCard1Bullets.Visible", true);
        cmd.set("#GuideCard1Bullet0.Visible", true);
        cmd.set("#GuideCard1Bullet0Text.Text", "/recruit — View your Asylum rank or browse recruits");
        cmd.set("#GuideCard1Bullet1.Visible", true);
        cmd.set("#GuideCard1Bullet1Text.Text", "/recruit bid <player> <gold> — Place a recruitment bid");
        cmd.set("#GuideCard1Bullet2.Visible", true);
        cmd.set("#GuideCard1Bullet2Text.Text", "/market — Open the player marketplace");
        cmd.set("#GuideCard1Bullet3.Visible", true);
        cmd.set("#GuideCard1Bullet3Text.Text", "/pay <player> <amount> — Send gold to another player");
        cmd.set("#GuideCard1Bullet4.Visible", true);
        cmd.set("#GuideCard1Bullet4Text.Text", "/bal — Check your gold balance");
        cmd.set("#GuideCard1Bullet5.Visible", true);
        cmd.set("#GuideCard1Bullet5Text.Text", "/char — Switch between your characters");

        // Controls & keybinds
        cmd.set("#GuideCard2.Visible", true);
        cmd.set("#GuideCard2Spacer.Visible", true);
        cmd.set("#GuideCard2Icon.TextSpans", Message.raw("K").color(Color.decode("#4ecdc4")));
        cmd.set("#GuideCard2Accent.Background", "#4ecdc4");
        cmd.set("#GuideCard2Title.Text", "CONTROLS & KEYBINDS");
        cmd.set("#GuideCard2Text.Text", "Key bindings for gameplay and navigation.");
        cmd.set("#GuideCard2Bullets.Visible", true);
        cmd.set("#GuideCard2Bullet0.Visible", true);
        cmd.set("#GuideCard2Bullet0Text.Text", "F — Interact with NPCs, objects, and mounts");
        cmd.set("#GuideCard2Bullet1.Visible", true);
        cmd.set("#GuideCard2Bullet1Text.Text", "ESC — Close any open panel or menu");
        cmd.set("#GuideCard2Bullet2.Visible", true);
        cmd.set("#GuideCard2Bullet2Text.Text", "T — Open chat to type messages and commands");
        cmd.set("#GuideCard2Bullet3.Visible", true);
        cmd.set("#GuideCard2Bullet3Text.Text", "Tab — Open the map view");
        cmd.set("#GuideCard2Bullet4.Visible", true);
        cmd.set("#GuideCard2Bullet4Text.Text", "1-9 — Switch hotbar slots for equipped items");
        cmd.set("#GuideCard2Bullet5.Visible", true);
        cmd.set("#GuideCard2Bullet5Text.Text", "I — Open inventory");

        // Tips & shortcuts card
        cmd.set("#GuideCard3.Visible", true);
        cmd.set("#GuideCard3Spacer.Visible", true);
        cmd.set("#GuideCard3Icon.TextSpans", Message.raw("*").color(Color.decode("#ffd700")));
        cmd.set("#GuideCard3Accent.Background", "#ffd700");
        cmd.set("#GuideCard3Title.Text", "TIPS & SHORTCUTS");
        cmd.set("#GuideCard3Text.Text", "Useful tips to get the most out of the panel.");
        cmd.set("#GuideCard3Bullets.Visible", true);
        cmd.set("#GuideCard3Bullet0.Visible", true);
        cmd.set("#GuideCard3Bullet0Text.Text", "Home page shows action items with unspent points and invitations");
        cmd.set("#GuideCard3Bullet1.Visible", true);
        cmd.set("#GuideCard3Bullet1Text.Text", "Click action items on the home screen to navigate directly");
        cmd.set("#GuideCard3Bullet2.Visible", true);
        cmd.set("#GuideCard3Bullet2Text.Text", "Stat and talent respec is free -- experiment with builds");
        cmd.set("#GuideCard3Bullet3.Visible", true);
        cmd.set("#GuideCard3Bullet3Text.Text", "Check the Guide section for detailed explanations of every system");
        cmd.set("#GuideCard3Bullet4.Visible", true);
        cmd.set("#GuideCard3Bullet4Text.Text", "Sidebar badges show counts for unspent points and invitations");

        // Getting help card
        cmd.set("#GuideCard4.Visible", true);
        cmd.set("#GuideCard4Spacer.Visible", true);
        cmd.set("#GuideCard4Icon.TextSpans", Message.raw("?").color(Color.decode("#ff9f43")));
        cmd.set("#GuideCard4Accent.Background", "#ff9f43");
        cmd.set("#GuideCard4Title.Text", "GETTING HELP");
        cmd.set("#GuideCard4Text.Text", "Resources for new and returning players.");
        cmd.set("#GuideCard4Bullets.Visible", true);
        cmd.set("#GuideCard4Bullet0.Visible", true);
        cmd.set("#GuideCard4Bullet0Text.Text", "/menu -- Open this panel from anywhere");
        cmd.set("#GuideCard4Bullet1.Visible", true);
        cmd.set("#GuideCard4Bullet1Text.Text", "Guide section covers combat, classes, honor, professions, and more");
        cmd.set("#GuideCard4Bullet2.Visible", true);
        cmd.set("#GuideCard4Bullet2Text.Text", "Ask in chat -- other players can help with quests and builds");
        cmd.set("#GuideCard4Bullet3.Visible", true);
        cmd.set("#GuideCard4Bullet3Text.Text", "Join a guild for organized group content and advice");

        // Stats row showing key server info
        cmd.set("#GuideStatsRow.Visible", true);
        cmd.set("#GuideStatsRowSpacer.Visible", true);
        cmd.set("#GuideStat0.Visible", true);
        cmd.set("#GuideStat0Label.Text", "CLASSES");
        cmd.set("#GuideStat0Value.TextSpans", Message.raw("4").color(Color.decode("#ff6b6b")));
        cmd.set("#GuideStat0Desc.Text", "Unique trees");
        cmd.set("#GuideStat1.Visible", true);
        cmd.set("#GuideStat1Label.Text", "PROFESSIONS");
        cmd.set("#GuideStat1Value.TextSpans", Message.raw("9").color(Color.decode("#4ecdc4")));
        cmd.set("#GuideStat1Desc.Text", "Crafting specs");
        cmd.set("#GuideStat2.Visible", true);
        cmd.set("#GuideStat2Label.Text", "HONOR RANKS");
        cmd.set("#GuideStat2Value.TextSpans", Message.raw("14").color(Color.decode("#d4af37")));
        cmd.set("#GuideStat2Desc.Text", "PvP titles");
        cmd.set("#GuideStat3.Visible", true);
        cmd.set("#GuideStat3Label.Text", "TRADESKILLS");
        cmd.set("#GuideStat3Value.TextSpans", Message.raw("6").color(Color.decode("#ffd700")));
        cmd.set("#GuideStat3Desc.Text", "Gathering");

        cmd.set("#FooterText.Text", "Open the Guide from the main panel for detailed explanations of every game system.");
    }

    public void handleToggle(PlayerRef playerRef, String setting, World world) {
        if (!"sbt_toggle".equals(setting)) return;
        try {
            Class<?> clazz = Class.forName("com.hcattributes.HC_AttributesPlugin");
            Object plugin = clazz.getMethod("getInstance").invoke(null);
            if (plugin == null) return;
            boolean current = (boolean) clazz.getMethod("isSctEnabled", java.util.UUID.class).invoke(plugin, playerRef.getUuid());
            clazz.getMethod("setSctEnabled", java.util.UUID.class, boolean.class, World.class, PlayerRef.class)
                .invoke(plugin, playerRef.getUuid(), !current, world, playerRef);
        } catch (Exception ignored) {}
    }

    private boolean isSctEnabled() {
        try {
            Class<?> clazz = Class.forName("com.hcattributes.HC_AttributesPlugin");
            Object plugin = clazz.getMethod("getInstance").invoke(null);
            if (plugin == null) return false;
            return (boolean) clazz.getMethod("isSctEnabled", java.util.UUID.class).invoke(plugin, playerRef.getUuid());
        } catch (Exception e) {
            return false;
        }
    }

    private void resetGuideElements(UICommandBuilder cmd) {
        cmd.set("#GuideHeaderImage.Visible", false);
        cmd.set("#GuideHeaderSpacer.Visible", false);
        cmd.set("#GuideIntroCard.Visible", false);
        cmd.set("#GuideIntroSpacer.Visible", false);

        for (int i = 0; i < MAX_CARDS; i++) {
            cmd.set("#GuideCard" + i + ".Visible", false);
            cmd.set("#GuideCard" + i + "Spacer.Visible", false);
            cmd.set("#GuideCard" + i + "Bullets.Visible", false);
            for (int j = 0; j < MAX_BULLETS_PER_CARD; j++) {
                cmd.set("#GuideCard" + i + "Bullet" + j + ".Visible", false);
            }
        }

        cmd.set("#GuideStatsRow.Visible", false);
        cmd.set("#GuideStatsRowSpacer.Visible", false);
        for (int i = 0; i < 4; i++) {
            cmd.set("#GuideStat" + i + ".Visible", false);
        }
        cmd.set("#GuideTipCard.Visible", false);

        // Reset settings toggles
        cmd.set("#SettingsToggle0.Visible", false);
        cmd.set("#SettingsToggle0Spacer.Visible", false);
    }
}
