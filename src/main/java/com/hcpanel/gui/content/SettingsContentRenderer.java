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
        buttons.add(new SidebarButton("Combat", "nav:settings:combat", null, SETTINGS_COLOR, isCombat));
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
            default -> renderCombatSettings(cmd, events);
        }

        cmd.set("#FooterText.Text", "Click TOGGLE to change a setting.");
    }

    private void renderCombatSettings(UICommandBuilder cmd, UIEventBuilder events) {
        boolean sctOn = isSctEnabled();

        String accentColor = sctOn ? "#4aff7f" : "#e74c3c";
        String statusText = sctOn ? "ON" : "OFF";
        String statusColor = sctOn ? "#4aff7f" : "#e74c3c";
        String btnText = sctOn ? "DISABLE" : "ENABLE";

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
