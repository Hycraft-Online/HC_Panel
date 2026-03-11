package com.hcpanel.gui.content;

import com.hcpanel.gui.UnifiedPanelGui.SidebarButton;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Direct imports from HC_Attributes (optional dependency)
import com.hcattributes.api.HC_AttributesAPI;
import com.hcattributes.models.Attribute;
import com.hcattributes.models.AttributeSnapshot;

// HC_Leveling and HC_Classes are optional - we use reflection to avoid compile-time dependency

// Direct imports from HC_Factions (optional dependency)
import com.hcfactions.HC_FactionsPlugin;
import com.hcfactions.models.PlayerData;

import com.hypixel.hytale.server.core.Message;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders Attributes module content into the unified panel.
 * Uses direct API calls instead of reflection for cross-plugin communication.
 */
public class AttributesContentRenderer {

    private final PlayerRef playerRef;

    // Attribute display info
    private static final Attribute[] PRIMARY_ATTRIBUTES = {
        Attribute.STRENGTH, Attribute.AGILITY, Attribute.INTELLECT, Attribute.SPIRIT, Attribute.VITALITY
    };
    private static final String[] ATTRIBUTE_ABBREV = {"STR", "AGI", "INT", "SPI", "VIT"};
    private static final String[] ATTRIBUTE_NAMES = {"Strength", "Agility", "Intellect", "Spirit", "Vitality"};

    public AttributesContentRenderer(PlayerRef playerRef) {
        this.playerRef = playerRef;
    }

    /**
     * Returns sidebar buttons for the Attributes module.
     */
    public List<SidebarButton> getSidebarButtons(String currentView) {
        List<SidebarButton> buttons = new ArrayList<>();

        boolean isStats = currentView == null || "stats".equals(currentView);
        boolean isAllocate = "allocate".equals(currentView);

        String badge = getStatPointsBadge();

        // Gold color (#d4af37) matches original HC_Attributes panel theme
        buttons.add(new SidebarButton("My Stats", "nav:attributes:stats", null, "#d4af37", isStats));
        buttons.add(new SidebarButton("Allocate", "nav:attributes:allocate", badge, "#d4af37", isAllocate));

        return buttons;
    }

    /**
     * Returns a badge string if the player has unspent stat points.
     */
    public String getStatPointsBadge() {
        if (!HC_AttributesAPI.isAvailable()) return null;

        int points = HC_AttributesAPI.getAvailableStatPoints(playerRef.getUuid());
        return points > 0 ? String.valueOf(points) : null;
    }

    /**
     * Renders content for the specified view.
     */
    public void renderContent(UICommandBuilder cmd, UIEventBuilder events, Store<EntityStore> store,
                               PlayerRef playerRef, String view) {
        if (!HC_AttributesAPI.isAvailable()) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "Attributes system not available.");
            return;
        }

        AttributeSnapshot snapshot = HC_AttributesAPI.getSnapshot(playerRef.getUuid());
        if (snapshot == null) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "Loading attribute data...");
            return;
        }

        // Default to stats view
        if (view == null) view = "stats";

        switch (view) {
            case "stats" -> renderStats(cmd, events, snapshot);
            case "allocate" -> renderAllocate(cmd, events, snapshot);
            default -> renderStats(cmd, events, snapshot);
        }
    }

    private void renderStats(UICommandBuilder cmd, UIEventBuilder events, AttributeSnapshot snapshot) {
        String classDisplay = getClassName();
        int playerLevel = getPlayerLevel();
        String factionId = getFactionId();

        // Hide the generic header section - we use the Attributes-specific header
        cmd.set("#HeaderSection.Visible", false);

        // === NEW: HERO CLASS SECTION ===
        cmd.set("#AttrHeroSection.Visible", true);

        // Level number (large)
        cmd.set("#AttrLevelNumber.Text", String.valueOf(playerLevel));

        // Class name (uppercase)
        cmd.set("#AttrClassName.Text", classDisplay.toUpperCase());

        // Player name
        cmd.set("#AttrPlayerName.Text", playerRef.getUsername());

        // Faction tag
        if (factionId != null) {
            var factionsPlugin = HC_FactionsPlugin.getInstance();
            if (factionsPlugin != null) {
                var faction = factionsPlugin.getFactionManager().getFaction(factionId);
                if (faction != null) {
                    cmd.set("#AttrFactionTag.TextSpans", Message.raw("[" + faction.getShortName() + "]").color(Color.decode(faction.getColorHex())));
                }
            }
        }

        // Stat points available banner
        int availablePoints = HC_AttributesAPI.getAvailableStatPoints(playerRef.getUuid());
        if (availablePoints > 0) {
            cmd.set("#AttrPointsBanner.Visible", true);
            cmd.set("#AttrPointsValue.Text", String.valueOf(availablePoints));
        }

        // === PRIMARY ATTRIBUTES CARD ===
        cmd.set("#PrimaryAttributesCard.Visible", true);
        cmd.set("#AttrPrimaryCardSpacer.Visible", true);

        for (int i = 0; i < PRIMARY_ATTRIBUTES.length; i++) {
            int value = snapshot.getAttributeValue(PRIMARY_ATTRIBUTES[i]);
            cmd.set("#AttrValue" + i + ".Text", String.valueOf(value));
            cmd.set("#AttrDerived" + i + ".Text", getAttributeDescription(i, value));
        }

        // === COMBAT / RESOURCES ROW ===
        cmd.set("#CombatResourcesRow.Visible", true);
        cmd.set("#AttrCombatResourcesSpacer.Visible", true);

        // Combat stats - show AP and the bonus damage it provides per hit
        int meleeAP = snapshot.getMeleeAttackPower();
        int rangedAP = snapshot.getRangedAttackPower();
        int spellPower = snapshot.getSpellPower();
        float critChance = snapshot.getCritChance();

        // Show bonus damage prominently (AP/2), with AP in smaller text below
        int meleeBonusDmg = Math.round(snapshot.getAttackPowerBonusDamage("melee"));
        int rangedBonusDmg = Math.round(snapshot.getAttackPowerBonusDamage("ranged"));
        int spellBonusDmg = Math.round(snapshot.getAttackPowerBonusDamage("magic"));

        cmd.set("#MeleeAPValue.Text", "+" + meleeBonusDmg);
        cmd.set("#MeleeAPDetail.Text", meleeAP + " AP");
        cmd.set("#RangedAPValue.Text", "+" + rangedBonusDmg);
        cmd.set("#RangedAPDetail.Text", rangedAP + " AP");
        cmd.set("#SpellPowerValue.Text", "+" + spellBonusDmg);
        cmd.set("#SpellPowerDetail.Text", spellPower + " SP");
        cmd.set("#CritChanceValue.Text", String.format("%.1f%%", critChance * 100));

        // Resource stats - use actual max values from the engine (includes all modifiers)
        float hpRegen = snapshot.getHealthRegen();
        float mpRegen = snapshot.getManaRegen();

        int totalHP = HC_AttributesAPI.getActualMaxHealth(playerRef.getUuid());
        int totalMP = HC_AttributesAPI.getActualMaxMana(playerRef.getUuid());

        // HP column - per-line breakdown
        int baseHP = 100;
        int vitality = snapshot.getAttributeValue(Attribute.VITALITY);
        int vitBonus = vitality * 10;
        int hpBeforeMultiplier = baseHP + vitBonus;
        // If total differs from base+vit, there's a class/talent multiplier
        String classLine = "";
        if (totalHP != hpBeforeMultiplier && hpBeforeMultiplier > 0) {
            float effectiveMultiplier = (float) totalHP / hpBeforeMultiplier;
            classLine = String.format("x%.2f Class/Talents", effectiveMultiplier);
        }

        cmd.set("#MaxHPValue.Text", String.valueOf(totalHP));
        cmd.set("#HPLineBase.Text", baseHP + " Base");
        cmd.set("#HPLineVit.Text", vitBonus > 0 ? "+" + vitBonus + " Vitality (" + vitality + ")" : "");
        cmd.set("#HPLineClass.Text", classLine);
        cmd.set("#HPLineRegen.Text", String.format("+%.1f/s regen", hpRegen));

        // MP column - per-line breakdown
        int baseMP = 50;
        int intellect = snapshot.getAttributeValue(Attribute.INTELLECT);
        int intBonus = intellect * 15;

        cmd.set("#MaxMPValue.Text", String.valueOf(totalMP));
        cmd.set("#MPLineBase.Text", baseMP + " Base");
        cmd.set("#MPLineInt.Text", intBonus > 0 ? "+" + intBonus + " Intellect (" + intellect + ")" : "");
        cmd.set("#MPLineRegen.Text", String.format("+%.1f/s regen", mpRegen));

        // === DEFENSE SECTION ===
        cmd.set("#DefenseSection.Visible", true);

        int armor = snapshot.getAttributeValue(Attribute.ARMOR);
        float physReduction = snapshot.getPhysicalDamageReduction() * 100;
        cmd.set("#ArmorValue.Text", String.valueOf(armor));
        cmd.set("#ArmorReduction.Text", String.format("%.1f%% reduction", physReduction));

        // Resistances
        cmd.set("#FireResValue.Text", String.valueOf(snapshot.getAttributeValue(Attribute.FIRE_RESISTANCE)));
        cmd.set("#IceResValue.Text", String.valueOf(snapshot.getAttributeValue(Attribute.ICE_RESISTANCE)));
        cmd.set("#LightningResValue.Text", String.valueOf(snapshot.getAttributeValue(Attribute.LIGHTNING_RESISTANCE)));
        cmd.set("#PoisonResValue.Text", String.valueOf(snapshot.getAttributeValue(Attribute.POISON_RESISTANCE)));
        cmd.set("#MagicResValue.Text", String.valueOf(snapshot.getAttributeValue(Attribute.MAGIC_RESISTANCE)));

        // Footer with build summary (reuse availablePoints from line 136)
        if (availablePoints > 0) {
            cmd.set("#FooterText.Text", availablePoints + " stat point" + (availablePoints > 1 ? "s" : "") + " available! Go to Allocate to spend them.");
        } else {
            // Show a brief build insight
            int str = snapshot.getAttributeValue(Attribute.STRENGTH);
            int agi = snapshot.getAttributeValue(Attribute.AGILITY);
            int intl = snapshot.getAttributeValue(Attribute.INTELLECT);
            String buildType;
            if (intl >= str && intl >= agi) {
                buildType = "Caster build (INT primary). Spell Power: " + snapshot.getSpellPower() + " SP.";
            } else if (agi >= str) {
                buildType = "Ranged/Crit build (AGI primary). Crit: " + String.format("%.1f%%", snapshot.getCritChance() * 100) + ".";
            } else {
                buildType = "Melee build (STR primary). Melee AP: " + snapshot.getMeleeAttackPower() + ".";
            }
            cmd.set("#FooterText.Text", buildType + " Use the Allocate tab to respec.");
        }
    }

    private void renderAllocate(UICommandBuilder cmd, UIEventBuilder events, AttributeSnapshot snapshot) {
        String classDisplay = getClassName();
        int playerLevel = getPlayerLevel();
        String factionId = getFactionId();

        // Hide the generic header section
        cmd.set("#HeaderSection.Visible", false);

        // === NEW: HERO CLASS SECTION (reused for allocate view) ===
        cmd.set("#AttrHeroSection.Visible", true);

        // Level number (large)
        cmd.set("#AttrLevelNumber.Text", String.valueOf(playerLevel));

        // Class name - show "ALLOCATE POINTS" instead
        cmd.set("#AttrClassName.Text", "ALLOCATE POINTS");

        // Player name shows class
        cmd.set("#AttrPlayerName.Text", classDisplay + " - Level " + playerLevel);

        // Faction tag
        if (factionId != null) {
            var factionsPlugin = HC_FactionsPlugin.getInstance();
            if (factionsPlugin != null) {
                var faction = factionsPlugin.getFactionManager().getFaction(factionId);
                if (faction != null) {
                    cmd.set("#AttrFactionTag.TextSpans", Message.raw("[" + faction.getShortName() + "]").color(Color.decode(faction.getColorHex())));
                }
            }
        }

        // Get available points early for the banner
        int availablePoints = HC_AttributesAPI.getAvailableStatPoints(playerRef.getUuid());

        // Show stat points banner
        if (availablePoints > 0) {
            cmd.set("#AttrPointsBanner.Visible", true);
            cmd.set("#AttrPointsValue.Text", String.valueOf(availablePoints));
        }

        // === ALLOCATE SECTION ===
        cmd.set("#AllocateSection.Visible", true);
        int totalAllocated = 0;

        // Calculate total allocated across all attributes
        for (Attribute attr : PRIMARY_ATTRIBUTES) {
            var attrValue = snapshot.getAttribute(attr);
            if (attrValue != null) {
                totalAllocated += attrValue.getAllocatedPoints();
            }
        }

        cmd.set("#AvailablePointsValue.Text", String.valueOf(availablePoints));
        cmd.set("#TotalAllocatedValue.Text", String.valueOf(totalAllocated));

        // Render each allocatable attribute
        for (int i = 0; i < PRIMARY_ATTRIBUTES.length; i++) {
            Attribute attr = PRIMARY_ATTRIBUTES[i];
            int totalValue = snapshot.getAttributeValue(attr);
            // Get allocated points from the AttributeValue breakdown
            var attrValue = snapshot.getAttribute(attr);
            int allocated = attrValue != null ? attrValue.getAllocatedPoints() : 0;

            // Set values using the new UI structure
            cmd.set("#AllocValue" + i + ".Text", String.valueOf(totalValue));
            cmd.set("#AllocAllocated" + i + ".Text", "(+" + allocated + " allocated)");

            // Bind allocation buttons
            events.addEventBinding(CustomUIEventBindingType.Activating, "#AllocBtn" + i + "Add1",
                EventData.of("Action", "action:allocate:" + attr.name() + ":1"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#AllocBtn" + i + "Add5",
                EventData.of("Action", "action:allocate:" + attr.name() + ":5"), false);
        }

        // Reset button
        events.addEventBinding(CustomUIEventBindingType.Activating, "#ResetBtn",
            EventData.of("Action", "action:allocate:reset:0"), false);

        // Footer
        cmd.set("#FooterText.Text", "Click +1 or +5 to allocate points. Reset returns all allocated points.");
    }

    private String getAttributeDescription(int index, int value) {
        return switch (index) {
            case 0 -> "+" + (value * 2) + " Melee AP (+" + value + " dmg)";  // STR: 2 melee AP, AP/2 bonus dmg
            case 1 -> "+" + (value * 2) + " Ranged AP, +" + String.format("%.1f", value / 20.0) + "% Crit";  // AGI: 2 RAP, 1% crit per 20
            case 2 -> "+" + (value * 2) + " SP (+" + value + " dmg), +" + (value * 15) + " MP";  // INT: 2 SP, SP/2 bonus dmg
            case 3 -> "+" + String.format("%.1f", value * 0.5) + " HP/s, +" + String.format("%.1f", value * 1.0) + " MP/s";  // SPI: 0.5 HP/s, 1.0 MP/s regen
            case 4 -> "+" + (value * 10) + " HP";  // VIT: 10 HP per point
            default -> "";
        };
    }

    private String getClassName() {
        try {
            Class<?> api = Class.forName("com.hcclasses.api.HC_ClassesAPI");
            boolean available = (boolean) api.getMethod("isAvailable").invoke(null);
            if (!available) return "Adventurer";
            Object playerClass = api.getMethod("getPlayerClass", java.util.UUID.class).invoke(null, playerRef.getUuid());
            if (playerClass == null) return "Adventurer";
            return (String) playerClass.getClass().getMethod("getDisplayName").invoke(playerClass);
        } catch (Exception e) {
            return "Adventurer";
        }
    }

    private int getPlayerLevel() {
        // Use reflection to check HC_Leveling availability (optional dependency)
        try {
            Class<?> apiClass = Class.forName("com.hcleveling.api.HC_LevelingAPI");
            var isAvailableMethod = apiClass.getMethod("isAvailable");
            Boolean isAvailable = (Boolean) isAvailableMethod.invoke(null);
            if (!isAvailable) return 1;

            var getLevelMethod = apiClass.getMethod("getPlayerLevel", java.util.UUID.class);
            return (Integer) getLevelMethod.invoke(null, playerRef.getUuid());
        } catch (Exception e) {
            // HC_Leveling not available
            return 1;
        }
    }

    private String getFactionId() {
        HC_FactionsPlugin factionsPlugin = HC_FactionsPlugin.getInstance();
        if (factionsPlugin == null) return null;

        PlayerData playerData = factionsPlugin.getPlayerDataRepository().getPlayerData(playerRef.getUuid());
        return playerData != null ? playerData.getFactionId() : null;
    }

    /**
     * Handles stat point allocation.
     */
    public boolean handleAllocation(PlayerRef playerRef, String actionData) {
        if (!HC_AttributesAPI.isAvailable()) return false;

        String[] parts = actionData.split(":");
        if (parts.length < 2) return false;

        String attribute = parts[0];
        int amount = Integer.parseInt(parts[1]);

        if ("reset".equals(attribute)) {
            return HC_AttributesAPI.resetAllocations(playerRef.getUuid());
        } else {
            Attribute attr = Attribute.valueOf(attribute);
            return HC_AttributesAPI.allocatePoints(
                playerRef.getUuid(), playerRef.getUsername(), attr, amount);
        }
    }
}
