package com.hcpanel.gui.content;

import com.hcpanel.gui.UnifiedPanelGui.SidebarButton;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders Guide content into the unified panel with rich formatting.
 * Uses styled cards, icons, bullet lists, and stat blocks.
 */
public class GuideContentRenderer {

    private static final String GUIDE_COLOR = "#4ecdc4";
    private static final int MAX_CARDS = 6;
    private static final int MAX_BULLETS_PER_CARD = 6;

    private final PlayerRef playerRef;

    public GuideContentRenderer(PlayerRef playerRef) {
        this.playerRef = playerRef;
    }

    /**
     * Returns sidebar buttons for the Guide module.
     */
    public List<SidebarButton> getSidebarButtons(String currentView) {
        List<SidebarButton> buttons = new ArrayList<>();

        boolean isOverview = currentView == null || "overview".equals(currentView);
        boolean isCombat = "combat".equals(currentView);
        boolean isAttributes = "attributes".equals(currentView);
        boolean isClasses = "classes".equals(currentView);
        boolean isEquipment = "equipment".equals(currentView);
        boolean isParty = "party".equals(currentView);
        boolean isFactions = "factions".equals(currentView);
        boolean isHonor = "honor".equals(currentView);

        buttons.add(new SidebarButton("Overview", "nav:guide:overview", null, GUIDE_COLOR, isOverview));
        buttons.add(new SidebarButton("Combat", "nav:guide:combat", null, GUIDE_COLOR, isCombat));
        buttons.add(new SidebarButton("Attributes", "nav:guide:attributes", null, GUIDE_COLOR, isAttributes));
        buttons.add(new SidebarButton("Classes", "nav:guide:classes", null, GUIDE_COLOR, isClasses));
        buttons.add(new SidebarButton("Equipment", "nav:guide:equipment", null, GUIDE_COLOR, isEquipment));
        buttons.add(new SidebarButton("Party", "nav:guide:party", null, GUIDE_COLOR, isParty));
        buttons.add(new SidebarButton("Factions", "nav:guide:factions", null, GUIDE_COLOR, isFactions));
        buttons.add(new SidebarButton("Honor", "nav:guide:honor", null, GUIDE_COLOR, isHonor));

        return buttons;
    }

    /**
     * Renders guide content for the specified section.
     */
    public void renderContent(UICommandBuilder cmd, UIEventBuilder events, String view) {
        // Hide the header section and content spacer, show the guide section
        cmd.set("#HeaderSection.Visible", false);
        cmd.set("#ContentSpacer.Visible", false);
        cmd.set("#GuideSection.Visible", true);

        // Reset all elements first
        resetGuideElements(cmd);

        // Default to overview
        if (view == null) view = "overview";

        switch (view) {
            case "combat" -> renderCombatGuide(cmd);
            case "attributes" -> renderAttributesGuide(cmd);
            case "classes" -> renderClassesGuide(cmd);
            case "equipment" -> renderEquipmentGuide(cmd);
            case "party" -> renderPartyGuide(cmd);
            case "factions" -> renderFactionsGuide(cmd);
            case "honor" -> renderHonorGuide(cmd);
            default -> renderOverviewGuide(cmd);
        }

        // Footer
        cmd.set("#FooterText.Text", "Select a topic from the sidebar to learn more.");
    }

    /**
     * Resets all guide UI elements to their default hidden state.
     */
    private void resetGuideElements(UICommandBuilder cmd) {
        // Hide header image
        cmd.set("#GuideHeaderImage.Visible", false);
        cmd.set("#GuideHeaderSpacer.Visible", false);

        // Hide intro card
        cmd.set("#GuideIntroCard.Visible", false);
        cmd.set("#GuideIntroSpacer.Visible", false);

        // Hide all content cards and their spacers
        for (int i = 0; i < MAX_CARDS; i++) {
            cmd.set("#GuideCard" + i + ".Visible", false);
            cmd.set("#GuideCard" + i + "Spacer.Visible", false);
            cmd.set("#GuideCard" + i + "Bullets.Visible", false);

            for (int j = 0; j < MAX_BULLETS_PER_CARD; j++) {
                cmd.set("#GuideCard" + i + "Bullet" + j + ".Visible", false);
            }
        }

        // Hide stats row
        cmd.set("#GuideStatsRow.Visible", false);
        cmd.set("#GuideStatsRowSpacer.Visible", false);
        for (int i = 0; i < 4; i++) {
            cmd.set("#GuideStat" + i + ".Visible", false);
        }

        // Hide tip card
        cmd.set("#GuideTipCard.Visible", false);
    }

    /**
     * Shows the header image for the current section.
     */
    private void showHeaderImage(UICommandBuilder cmd, String sectionName) {
        cmd.set("#GuideHeaderImage.Visible", true);
        cmd.set("#GuideHeaderSpacer.Visible", true);
        String imagePath = "GuideHeaders/Guide" + sectionName + ".png";
        PatchStyle patchStyle = new PatchStyle(Value.of(imagePath));
        cmd.setObject("#GuideHeaderImage.Background", patchStyle);
    }

    /**
     * Shows the intro card with welcome text.
     */
    private void showIntro(UICommandBuilder cmd, String text) {
        cmd.set("#GuideIntroCard.Visible", true);
        cmd.set("#GuideIntroSpacer.Visible", true);
        cmd.set("#GuideIntroText.Text", text);
    }

    /**
     * Shows a content card with icon, title, text, and optional bullets.
     */
    private void showCard(UICommandBuilder cmd, int index, String icon, String title,
                          String text, String accentColor, String... bullets) {
        String prefix = "#GuideCard" + index;

        cmd.set(prefix + ".Visible", true);
        cmd.set(prefix + "Spacer.Visible", true);
        cmd.set(prefix + "Icon.Text", icon);
        cmd.set(prefix + "Title.Text", title);
        cmd.set(prefix + "Text.Text", text);

        // Set accent color
        if (accentColor != null) {
            cmd.set(prefix + "Accent.Background", accentColor);
            cmd.set(prefix + "Icon.Style.TextColor", accentColor);
        }

        // Show bullets if provided
        if (bullets != null && bullets.length > 0) {
            cmd.set(prefix + "Bullets.Visible", true);
            for (int i = 0; i < bullets.length && i < MAX_BULLETS_PER_CARD; i++) {
                cmd.set(prefix + "Bullet" + i + ".Visible", true);
                cmd.set(prefix + "Bullet" + i + "Text.Text", bullets[i]);
            }
        }
    }

    /**
     * Shows a stat card in the stats row.
     */
    private void showStat(UICommandBuilder cmd, int index, String label, String value,
                          String desc, String valueColor) {
        cmd.set("#GuideStatsRow.Visible", true);
        cmd.set("#GuideStatsRowSpacer.Visible", true);
        cmd.set("#GuideStat" + index + ".Visible", true);
        cmd.set("#GuideStat" + index + "Label.Text", label);
        cmd.set("#GuideStat" + index + "Value.Text", value);
        cmd.set("#GuideStat" + index + "Desc.Text", desc);
        if (valueColor != null) {
            cmd.set("#GuideStat" + index + "Value.Style.TextColor", valueColor);
        }
    }

    // ===== GUIDE CONTENT RENDERERS =====

    private void renderOverviewGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Overview");

        showCard(cmd, 0, "X", "COMBAT & ATTRIBUTES",
            "Master the fundamentals of battle and character building.",
            "#4ecdc4",
            "Learn damage types, weapons, and defense",
            "Allocate stat points to shape your playstyle",
            "Choose a class with unique talents"
        );

        showCard(cmd, 1, "+", "EQUIPMENT & DUNGEONS",
            "Gear up and conquer challenging content with friends.",
            "#ffd700",
            "Collect loot with random affixes and rarities",
            "Form parties of up to 8 players",
            "Share lives in dungeon runs"
        );

        showCard(cmd, 2, "!", "FACTIONS & HONOR",
            "Join the war between factions and rise through PvP ranks.",
            "#e74c3c",
            "Create or join a guild",
            "Battle for territory control",
            "Earn honor and exclusive rewards"
        );

        showCard(cmd, 3, "?", "QUICK START",
            "Essential commands to get you going.",
            "#9b59b6",
            "/menu - Open this panel anytime",
            "/party - Find or create a group",
            "/guild - Manage your guild"
        );
    }

    private void renderCombatGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Combat");

        showCard(cmd, 0, "X", "DAMAGE TYPES",
            "Two damage categories with different scaling and mitigation.",
            "#e74c3c",
            "Physical - Scales with Strength, reduced by Armor",
            "Magical - Scales with Intelligence, reduced by Magic Resist"
        );

        showCard(cmd, 1, "/", "WEAPONS",
            "Each weapon type has unique characteristics.",
            "#ffd700",
            "Swords/Axes - Balanced physical damage",
            "Daggers - Fast, scales with Agility",
            "Maces - Slow but powerful",
            "Staves - Magical damage",
            "Bows - Ranged physical"
        );

        showCard(cmd, 2, "O", "DEFENSE",
            "Stack the right stats to survive.",
            "#4a9eff",
            "Armor - Reduces physical damage",
            "Magic Resistance - Reduces magical damage",
            "Both increase through equipment"
        );

        showCard(cmd, 3, "+", "RECOVERY",
            "Keep yourself alive between fights.",
            "#4aff7f",
            "Spirit - Boosts HP and MP regeneration",
            "Vitality - Increases max health pool"
        );
    }

    private void renderAttributesGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Attributes");

        showCard(cmd, 0, "S", "STRENGTH",
            "Melee power for warriors and frontline fighters.",
            "#ff6b6b",
            "+2 Melee Attack Power per point",
            "Primary: Warrior"
        );

        showCard(cmd, 1, "A", "AGILITY",
            "Speed and precision for rogues and rangers.",
            "#4ecdc4",
            "+2 Ranged Attack Power per point",
            "+1% Critical Chance per 20 points",
            "Primary: Rogue, Ranger"
        );

        showCard(cmd, 2, "I", "INTELLIGENCE",
            "Arcane power for mages and casters.",
            "#45b7d1",
            "+2 Spell Power per point",
            "+15 Maximum Mana per point",
            "Primary: Mage"
        );

        showCard(cmd, 3, "P", "SPIRIT",
            "Regeneration for sustained combat.",
            "#9b59b6",
            "+0.5 HP Regen per second",
            "+1.0 MP Regen per second"
        );

        showCard(cmd, 4, "V", "VITALITY",
            "Survivability for all builds.",
            "#e67e22",
            "+10 Maximum Health per point",
            "Recommended for everyone"
        );
    }

    private void renderClassesGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Classes");

        showCard(cmd, 0, "W", "WARRIOR",
            "Frontline tank with heavy armor and powerful strikes.",
            "#ff6b6b",
            "Damage reduction talents",
            "Powerful melee abilities",
            "Stats: Strength + Vitality"
        );

        showCard(cmd, 1, "R", "ROGUE",
            "Deadly assassin with burst damage and evasion.",
            "#4ecdc4",
            "Critical strike focus",
            "Dodge and stealth talents",
            "Stats: Agility"
        );

        showCard(cmd, 2, "H", "RANGER",
            "Ranged specialist with precision and mobility.",
            "#4aff7f",
            "Ranged weapon mastery",
            "Movement speed bonuses",
            "Stats: Agility"
        );

        showCard(cmd, 3, "M", "MAGE",
            "Arcane caster with devastating AoE spells. (Coming Soon)",
            "#8b5cf6",
            "Elemental damage talents",
            "Mana efficiency bonuses",
            "Stats: Intelligence"
        );

        showCard(cmd, 4, "*", "TALENTS",
            "Customize your class with talent points.",
            "#ffd700",
            "Earn 1 point per level",
            "Access via /menu > Character",
            "Free respec anytime"
        );
    }

    private void renderEquipmentGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Equipment");

        // Rarity stat cards
        showStat(cmd, 0, "COMMON", "0", "No affixes", "#888888");
        showStat(cmd, 1, "UNCOMMON", "1", "Fixed level", "#4aff7f");
        showStat(cmd, 2, "RARE", "1-2", "+/- 5 lvls", "#4a9eff");
        showStat(cmd, 3, "EPIC", "2-3", "+/- 10 lvls", "#8b5cf6");

        showCard(cmd, 0, "L", "LEGENDARY",
            "The rarest tier with maximum power.",
            "#ff9f43",
            "3-4 affixes, +/- 15 level variance",
            "Drops from dungeon bosses only"
        );

        showCard(cmd, 1, "<", "PREFIXES",
            "Offensive affixes before item name.",
            "#ffd700",
            "Heavy - Damage boost",
            "Berserker - Critical chance",
            "Arcane - Spell power",
            "Godslayer - Max damage"
        );

        showCard(cmd, 2, ">", "SUFFIXES",
            "Defensive affixes after item name.",
            "#4a9eff",
            "of the Bear - Health",
            "of Protection - Armor",
            "of the Eternal - All resists"
        );

        showCard(cmd, 3, "?", "LOOT TIPS",
            "Maximize your gear drops.",
            "#4ecdc4",
            "Higher enemy level = better drops",
            "Dungeon bosses drop best gear",
            "Item level affects base stats"
        );
    }

    private void renderPartyGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Party");

        showCard(cmd, 0, "P", "BASIC COMMANDS",
            "Form and manage your party.",
            "#4a9eff",
            "/party create - Start a party",
            "/party - Open browser",
            "/party accept - Join invite",
            "/party leave - Leave party"
        );

        showCard(cmd, 1, "!", "LEADER COMMANDS",
            "Additional commands for party leaders.",
            "#ffd700",
            "/party invite <name> - Invite player",
            "/party kick <name> - Remove member",
            "/party disband - Disband party"
        );

        showCard(cmd, 2, "+", "DUNGEON LIVES",
            "Shared respawn system in dungeons.",
            "#4aff7f",
            "Each member adds 3 lives to pool",
            "Any death costs 1 shared life",
            "Run ends when pool is empty"
        );

        showCard(cmd, 3, "X", "RESTRICTIONS",
            "Faction rules for parties.",
            "#e74c3c",
            "Opposing factions cannot group",
            "Neutral players can join anyone",
            "Max 8 members per party"
        );
    }

    private void renderFactionsGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Factions");

        showCard(cmd, 0, "G", "JOINING A GUILD",
            "Guilds are player groups within a faction.",
            "#4a9eff",
            "Ask a guild leader for invite",
            "Or create via /menu > Factions",
            "Choose name and 3-4 letter tag"
        );

        showCard(cmd, 1, "*", "GUILD RANKS",
            "Permission levels within guilds.",
            "#ffd700",
            "Leader - Full control",
            "Officer - Invite and kick",
            "Member - Standard access"
        );

        showCard(cmd, 2, "!", "WARFARE",
            "Fight for your faction's dominance.",
            "#e74c3c",
            "Kill enemies for Honor points",
            "Control territory for bonuses",
            "Join scheduled PvP events"
        );

        showCard(cmd, 3, "+", "BENEFITS",
            "Perks of guild membership.",
            "#4aff7f",
            "Private guild chat",
            "Territory control bonuses",
            "Organized group content"
        );
    }

    private void renderHonorGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Honor");

        // Honor stat cards
        showStat(cmd, 0, "WEEKLY", "Honor", "Resets Sunday", "#ffd700");
        showStat(cmd, 1, "BRACKETS", "1-10", "By weekly honor", "#4aff7f");
        showStat(cmd, 2, "RANKS", "14", "Private to GM", "#8b5cf6");
        showStat(cmd, 3, "DECAY", "20%", "RP per week", "#e74c3c");

        showCard(cmd, 0, "H", "WEEKLY HONOR",
            "Earned from PvP, determines your bracket.",
            "#ffd700",
            "Kill enemy faction players",
            "Resets Sunday midnight UTC",
            "Higher honor = better bracket"
        );

        showCard(cmd, 1, "B", "BRACKETS",
            "Your bracket sets weekly Rank Points earned.",
            "#4aff7f",
            "Bracket 1: +13,000 RP",
            "Bracket 5: +6,500 RP",
            "Bracket 10: +1,500 RP"
        );

        showCard(cmd, 2, "R", "RANKS",
            "14 ranks with unique titles.",
            "#8b5cf6",
            "Rank 1: Private",
            "Rank 7: Knight",
            "Rank 14: Grand Marshal"
        );

        showCard(cmd, 3, "$", "HONOR SHOP",
            "Separate spendable currency.",
            "#ff9f43",
            "Earned from PvP kills",
            "Buy cosmetics and gear",
            "Does not decay"
        );
    }
}
