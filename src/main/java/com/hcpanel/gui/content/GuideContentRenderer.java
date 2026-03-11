package com.hcpanel.gui.content;

import com.hcpanel.gui.UnifiedPanelGui.SidebarButton;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders Guide content into the unified panel with rich formatting.
 * Covers every game system in detail: combat, attributes, classes, equipment,
 * parties, factions, honor, professions, and recruitment.
 */
public class GuideContentRenderer {

    private static final String GUIDE_COLOR = "#4ecdc4";
    private static final int MAX_CARDS = 6;
    private static final int MAX_BULLETS_PER_CARD = 6;

    private final PlayerRef playerRef;

    public GuideContentRenderer(PlayerRef playerRef) {
        this.playerRef = playerRef;
    }

    public List<SidebarButton> getSidebarButtons(String currentView) {
        List<SidebarButton> buttons = new ArrayList<>();

        String v = currentView != null ? currentView : "overview";

        buttons.add(new SidebarButton("Overview", "nav:guide:overview", null, GUIDE_COLOR, "overview".equals(v)));
        buttons.add(new SidebarButton("Combat", "nav:guide:combat", null, "#e74c3c", "combat".equals(v)));
        buttons.add(new SidebarButton("Attributes", "nav:guide:attributes", null, "#8b5cf6", "attributes".equals(v)));
        buttons.add(new SidebarButton("Classes", "nav:guide:classes", null, "#ff6b6b", "classes".equals(v)));
        buttons.add(new SidebarButton("Equipment", "nav:guide:equipment", null, "#ffd700", "equipment".equals(v)));
        buttons.add(new SidebarButton("Party", "nav:guide:party", null, "#4a9eff", "party".equals(v)));
        buttons.add(new SidebarButton("Factions", "nav:guide:factions", null, "#4a9eff", "factions".equals(v)));
        buttons.add(new SidebarButton("Honor", "nav:guide:honor", null, "#d4af37", "honor".equals(v)));
        buttons.add(new SidebarButton("Professions", "nav:guide:professions", null, "#4ecdc4", "professions".equals(v)));
        buttons.add(new SidebarButton("Recruitment", "nav:guide:recruitment", null, "#ffd700", "recruitment".equals(v)));

        return buttons;
    }

    public void renderContent(UICommandBuilder cmd, UIEventBuilder events, String view) {
        cmd.set("#HeaderSection.Visible", false);
        cmd.set("#ContentSpacer.Visible", false);
        cmd.set("#GuideSection.Visible", true);
        resetGuideElements(cmd);

        if (view == null) view = "overview";

        switch (view) {
            case "combat" -> {
                renderCombatGuide(cmd);
                cmd.set("#FooterText.Text", "See the Attributes guide for stat scaling details, or Classes for talent synergies.");
            }
            case "attributes" -> {
                renderAttributesGuide(cmd);
                cmd.set("#FooterText.Text", "Allocate points in /menu > Character > Allocate. Respec is free anytime.");
            }
            case "classes" -> {
                renderClassesGuide(cmd);
                cmd.set("#FooterText.Text", "Choose your class with /class, then spend talent points in /menu > Character > Talents.");
            }
            case "equipment" -> {
                renderEquipmentGuide(cmd);
                cmd.set("#FooterText.Text", "Farm dungeons for the best loot. Check /menu > Character to see your combat stats.");
            }
            case "party" -> {
                renderPartyGuide(cmd);
                cmd.set("#FooterText.Text", "Use /party create to start a group, or /party to browse open parties.");
            }
            case "factions" -> {
                renderFactionsGuide(cmd);
                cmd.set("#FooterText.Text", "Manage your guild in /menu > Guild. Use /faction to choose a side.");
            }
            case "honor" -> {
                renderHonorGuide(cmd);
                cmd.set("#FooterText.Text", "Track your standing in /menu > Honor. PvP daily for the best weekly bracket.");
            }
            case "professions" -> {
                renderProfessionsGuide(cmd);
                cmd.set("#FooterText.Text", "View your skill levels in /menu > Skills. Use /profession choose to specialize.");
            }
            case "recruitment" -> {
                renderRecruitmentGuide(cmd);
                cmd.set("#FooterText.Text", "Check your rank and bids in /menu > Recruitment. Challenge the Asylum to improve.");
            }
            default -> {
                renderOverviewGuide(cmd);
                cmd.set("#FooterText.Text", "Select a topic from the sidebar to learn about any game system in detail.");
            }
        }
    }

    // ─── UI Helpers ───

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
    }

    private void showHeaderImage(UICommandBuilder cmd, String sectionName) {
        cmd.set("#GuideHeaderImage.Visible", true);
        cmd.set("#GuideHeaderSpacer.Visible", true);
        PatchStyle patchStyle = new PatchStyle(Value.of("GuideHeaders/Guide" + sectionName + ".png"));
        cmd.setObject("#GuideHeaderImage.Background", patchStyle);
    }

    private void showIntro(UICommandBuilder cmd, String text) {
        cmd.set("#GuideIntroCard.Visible", true);
        cmd.set("#GuideIntroSpacer.Visible", true);
        cmd.set("#GuideIntroText.Text", text);
    }

    private void showCard(UICommandBuilder cmd, int index, String icon, String title,
                          String text, String accentColor, String... bullets) {
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
            for (int i = 0; i < bullets.length && i < MAX_BULLETS_PER_CARD; i++) {
                cmd.set(prefix + "Bullet" + i + ".Visible", true);
                cmd.set(prefix + "Bullet" + i + "Text.Text", bullets[i]);
            }
        }
    }

    private void showStat(UICommandBuilder cmd, int index, String label, String value,
                          String desc, String valueColor) {
        cmd.set("#GuideStatsRow.Visible", true);
        cmd.set("#GuideStatsRowSpacer.Visible", true);
        cmd.set("#GuideStat" + index + ".Visible", true);
        cmd.set("#GuideStat" + index + "Label.Text", label);
        cmd.set("#GuideStat" + index + "Value.Text", value);
        cmd.set("#GuideStat" + index + "Desc.Text", desc);
        if (valueColor != null) {
            cmd.set("#GuideStat" + index + "Value.TextSpans", Message.raw(value).color(Color.decode(valueColor)));
        }
    }

    private void showTip(UICommandBuilder cmd, String text) {
        cmd.set("#GuideTipCard.Visible", true);
        cmd.set("#GuideTipText.Text", text);
    }

    // ===== GUIDE SECTIONS =====

    private void renderOverviewGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Overview");

        showIntro(cmd, "Welcome to Hycraft Online! This guide covers every game system "
            + "to help you get started. New player? Start by choosing a faction with "
            + "/faction, then pick a class with /class. Select any topic in the sidebar for details.");

        showStat(cmd, 0, "STEP 1", "/faction", "Pick a side", "#ff6b6b");
        showStat(cmd, 1, "STEP 2", "/class", "Choose class", "#8b5cf6");
        showStat(cmd, 2, "STEP 3", "/menu", "Spend points", "#ffd700");
        showStat(cmd, 3, "STEP 4", "Guild", "Join or create", "#4a9eff");

        showCard(cmd, 0, "X", "COMBAT & CHARACTER",
            "Build your character with attributes, classes, and talent trees.",
            "#e74c3c",
            "Two damage types: Physical and Magical",
            "Five core attributes: STR, AGI, INT, SPI, VIT",
            "4 Classes: Warrior, Rogue, Ranger, Mage",
            "9 talent tiers per class with branching paths",
            "Free respec for both stats and talents"
        );

        showCard(cmd, 1, "+", "EQUIPMENT & LOOT",
            "Gear up with randomized loot dropped by enemies and bosses.",
            "#ffd700",
            "5 rarity tiers: Common to Legendary",
            "Items roll random prefixes (offense) and suffixes (defense)",
            "Item level determines base stats",
            "Legendary gear only drops from dungeon bosses"
        );

        showCard(cmd, 2, "P", "PARTIES & DUNGEONS",
            "Group up for dungeons, quests, and PvP with parties of up to 8.",
            "#4a9eff",
            "Shared dungeon lives (3 per member)",
            "Party leader can invite, kick, and promote",
            "Faction restrictions apply to party composition",
            "Personal loot drops for each player"
        );

        showCard(cmd, 3, "!", "FACTIONS & HONOR",
            "Join a faction, create a guild, and climb the PvP rankings.",
            "#d4af37",
            "Choose a faction and create or join a guild",
            "5 guild ranks: Leader, Officer, Senior, Member, Recruit",
            "14 Honor ranks from Private to Grand Marshal",
            "Weekly brackets determine Rank Point gains"
        );

        showCard(cmd, 4, "#", "PROFESSIONS & TRADESKILLS",
            "Craft powerful items and gather rare resources from the world.",
            "#4ecdc4",
            "9 crafting professions (1 main, rest capped)",
            "6 gathering tradeskills with bonus yield per level",
            "Higher skill = better recipes and resources"
        );

        showCard(cmd, 5, "R", "RECRUITMENT SYSTEM",
            "Prove yourself in the Asylum, then get recruited by guilds.",
            "#ff9f43",
            "Complete the Asylum boss for a permanent rank",
            "Guilds bid gold to recruit top players",
            "Accept or decline bids in /menu > Recruitment"
        );

        showTip(cmd, "TIP: Open this panel anytime with /menu or the Menu item in your "
            + "inventory. You can allocate stat points, manage talents, view your guild, "
            + "and track honor all from one place.");
    }

    private void renderCombatGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Combat");

        showIntro(cmd, "Combat in Hycraft uses two damage types, each with its own scaling "
            + "attribute and mitigation stat. Understanding this system is key to building "
            + "an effective character.");

        showCard(cmd, 0, "X", "PHYSICAL DAMAGE",
            "Dealt by melee weapons and bows. The primary damage type for Warriors, Rogues, and Rangers.",
            "#e74c3c",
            "Scales with Strength (melee) or Agility (ranged)",
            "+2 Attack Power per point of STR or AGI",
            "Mitigated by the target's Armor rating",
            "Armor reduces damage by a percentage (diminishing)"
        );

        showCard(cmd, 1, "~", "MAGICAL DAMAGE",
            "Dealt by staves and spell abilities. The primary damage type for Mages.",
            "#8b5cf6",
            "Scales with Intelligence",
            "+2 Spell Power per point of INT",
            "Mitigated by the target's Magic Resistance",
            "Elemental subtypes: Fire, Ice, Lightning, Poison"
        );

        showCard(cmd, 2, "/", "WEAPON TYPES",
            "Each weapon type has unique attack speed, range, and damage characteristics.",
            "#ffd700",
            "Swords / Axes  -  Balanced speed and damage",
            "Daggers  -  Very fast, lower damage per hit",
            "Maces  -  Slow and powerful, armor piercing",
            "Staves  -  Magical damage, medium speed",
            "Bows  -  Ranged physical, scales with AGI"
        );

        showCard(cmd, 3, "%", "CRITICAL STRIKES",
            "Crits deal bonus damage and are key to burst builds.",
            "#ff9f43",
            "+1% Critical Chance per 20 Agility",
            "Base crit multiplier: 150% damage",
            "Some talents increase crit chance or damage",
            "Displayed as yellow numbers (SCT)"
        );

        showCard(cmd, 4, "O", "DEFENSE & MITIGATION",
            "Reduce incoming damage with the right defensive stats.",
            "#4a9eff",
            "Armor  -  Reduces physical damage taken",
            "Magic Resist  -  Reduces magical damage taken",
            "Both come primarily from equipment",
            "Diminishing returns at high values"
        );

        showCard(cmd, 5, "+", "RECOVERY & SUSTAIN",
            "Stay alive between fights with regeneration.",
            "#4aff7f",
            "Spirit  -  +0.5 HP/s and +1.0 MP/s per point",
            "Vitality  -  +10 Max HP per point",
            "Food and potions give temporary boosts",
            "Out-of-combat regen is significantly faster"
        );

        showTip(cmd, "TIP: Match your attributes to your damage type. Melee fighters "
            + "want STR, ranged fighters want AGI, casters want INT. Everyone benefits "
            + "from VIT for survivability and SPI for regeneration.");
    }

    private void renderAttributesGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Attributes");

        showIntro(cmd, "Every character has five core attributes. You earn stat points "
            + "as you level up and can allocate them freely. Respec is free anytime "
            + "via the Character panel.");

        showCard(cmd, 0, "S", "STRENGTH",
            "Raw melee power. The primary stat for Warriors.",
            "#ff6b6b",
            "+2 Melee Attack Power per point",
            "Increases melee weapon damage directly",
            "Best paired with Vitality for frontline builds",
            "Recommended: Warriors, melee Rogues"
        );

        showCard(cmd, 1, "A", "AGILITY",
            "Speed, precision, and ranged power. Primary for Rogues and Rangers.",
            "#4ecdc4",
            "+2 Ranged Attack Power per point",
            "+1% Critical Strike Chance per 20 AGI",
            "Affects both ranged and dagger damage",
            "Recommended: Rangers, Rogues"
        );

        showCard(cmd, 2, "I", "INTELLIGENCE",
            "Arcane mastery and mana reserves. Primary for Mages.",
            "#45b7d1",
            "+2 Spell Power per point",
            "+15 Maximum Mana per point",
            "Scales all magical ability damage",
            "Recommended: Mages"
        );

        showCard(cmd, 3, "P", "SPIRIT",
            "Regeneration for sustained fights. Valuable for every build.",
            "#9b59b6",
            "+0.5 HP Regeneration per second",
            "+1.0 MP Regeneration per second",
            "Reduces downtime between encounters",
            "Recommended: Healers, mana-heavy builds"
        );

        showCard(cmd, 4, "V", "VITALITY",
            "Survivability and raw health. Universal defensive stat.",
            "#e67e22",
            "+10 Maximum Health per point",
            "Works for every class and build",
            "Essential for tanking and PvP",
            "Recommended: Everyone (especially tanks)"
        );

        showCard(cmd, 5, "^", "LEVELING & EXPERIENCE",
            "Earn experience to level up and unlock more stat and talent points.",
            "#ffd700",
            "Kill enemies and complete quests for XP",
            "Each level grants 5 stat points to allocate",
            "Each level grants 1 talent point for your class",
            "Higher talent tiers require specific levels",
            "Current level cap: check /menu for your level"
        );

        showStat(cmd, 0, "PER LEVEL", "5", "Stat points", "#ffd700");
        showStat(cmd, 1, "RESPEC", "Free", "Anytime", "#4aff7f");
        showStat(cmd, 2, "PANEL", "/menu", "Character", "#4a9eff");
        showStat(cmd, 3, "SCALING", "Linear", "No soft caps", "#8b5cf6");
    }

    private void renderClassesGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Classes");

        showIntro(cmd, "Choose your class to unlock a unique talent tree. Each class "
            + "excels in a different role. Talent points are earned per level and "
            + "can be reset for free anytime.");

        showCard(cmd, 0, "W", "WARRIOR",
            "Melee brawler. High survivability with damage reduction and powerful strikes. "
            + "Excels as a frontline tank or sustained damage dealer.",
            "#ff6b6b",
            "Primary: Strength + Vitality",
            "Damage reduction and taunt talents",
            "Heavy armor proficiency",
            "Powerful cleave and charge abilities"
        );

        showCard(cmd, 1, "R", "ROGUE",
            "Burst assassin. Deadly critical strikes and evasive maneuvers. "
            + "Thrives in quick engagements and ambush tactics.",
            "#4ecdc4",
            "Primary: Agility",
            "Critical strike and combo talents",
            "Dodge, stealth, and poison abilities",
            "Excels at single-target burst damage"
        );

        showCard(cmd, 2, "H", "RANGER",
            "Ranged specialist. Precision attacks from distance with high mobility. "
            + "Controls the battlefield with kiting and area denial.",
            "#4aff7f",
            "Primary: Agility",
            "Ranged weapon mastery talents",
            "Movement speed and disengage abilities",
            "Strong in PvP and group play"
        );

        showCard(cmd, 3, "M", "MAGE",
            "Arcane caster. Devastating area damage and elemental specialization. "
            + "High damage ceiling but requires mana management.",
            "#8b5cf6",
            "Primary: Intelligence + Spirit",
            "Elemental specialization talents",
            "AoE damage and crowd control",
            "Mana efficiency and regen bonuses"
        );

        showCard(cmd, 4, "*", "TALENT SYSTEM",
            "Customize your class with talent points earned each level.",
            "#ffd700",
            "1 talent point earned per character level",
            "9 tiers of talents to unlock",
            "Higher tiers require specific levels",
            "Some talents have prerequisites",
            "Free respec via /menu > Character > Talents"
        );

        showCard(cmd, 5, "?", "GROUP ROLES & SYNERGIES",
            "Each class fills a role in group content. Build your party around complementary strengths.",
            "#ff9f43",
            "Warrior  -  Tank, frontline damage soak",
            "Rogue  -  Burst DPS, assassinations",
            "Ranger  -  Sustained ranged DPS, kiting",
            "Mage  -  AoE damage, crowd control",
            "Mix classes for the strongest party composition"
        );

        showTip(cmd, "TIP: You can preview talent effects by hovering over nodes in the "
            + "talent tree. Locked talents show what level and prerequisites you need.");
    }

    private void renderEquipmentGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Equipment");

        showIntro(cmd, "Equipment drops from enemies with random affixes and rarity tiers. "
            + "Higher level enemies and dungeon bosses drop better gear. Item level "
            + "determines base stats.");

        showStat(cmd, 0, "COMMON", "0 affixes", "Fixed level", "#888888");
        showStat(cmd, 1, "UNCOMMON", "1 affix", "Fixed level", "#4aff7f");
        showStat(cmd, 2, "RARE", "1-2 affixes", "+/-5 levels", "#4a9eff");
        showStat(cmd, 3, "EPIC", "2-3 affixes", "+/-10 levels", "#8b5cf6");

        showCard(cmd, 0, "L", "LEGENDARY TIER",
            "The rarest and most powerful equipment tier. Only drops from dungeon bosses.",
            "#ff9f43",
            "3 to 4 random affixes",
            "+/- 15 level variance from drop source",
            "Guaranteed from final dungeon bosses",
            "Highest possible base stat values"
        );

        showCard(cmd, 1, "<", "PREFIXES (OFFENSIVE)",
            "Offensive bonuses added before the item name. Examples:",
            "#ffd700",
            "Heavy  -  Increased weapon damage",
            "Berserker  -  Critical strike chance",
            "Arcane  -  Bonus spell power",
            "Godslayer  -  Maximum physical damage",
            "Swift  -  Attack speed bonus"
        );

        showCard(cmd, 2, ">", "SUFFIXES (DEFENSIVE)",
            "Defensive bonuses added after the item name. Examples:",
            "#4a9eff",
            "of the Bear  -  Bonus health",
            "of Protection  -  Increased armor",
            "of the Owl  -  Bonus mana",
            "of the Eternal  -  All elemental resistances",
            "of Regeneration  -  Health regen"
        );

        showCard(cmd, 3, "=", "ITEM LEVEL & SCALING",
            "Item level determines the base stats of equipment before affixes.",
            "#4ecdc4",
            "Higher enemy level = higher item level drops",
            "Item level affects base armor, damage, and stats",
            "Affixes scale with item level as well",
            "Equip the highest item level gear you can find"
        );

        showTip(cmd, "TIP: Farm dungeon bosses for the best loot. Legendary items only "
            + "drop from boss kills. Higher rarity items can roll level variance, so a "
            + "Rare from a high-level boss can outperform an Epic from a low-level one.");
    }

    private void renderPartyGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Party");

        showIntro(cmd, "Parties let you group up with other players for dungeons, "
            + "quests, and PvP. Up to 8 players can join a party. Party members "
            + "share dungeon lives and see each other on the map.");

        showCard(cmd, 0, "P", "CREATING & JOINING",
            "Basic party commands for all players.",
            "#4a9eff",
            "/party create  -  Start a new party",
            "/party  -  Browse open parties",
            "/party accept  -  Accept a pending invite",
            "/party leave  -  Leave your current party"
        );

        showCard(cmd, 1, "!", "LEADER COMMANDS",
            "Additional commands available to the party leader.",
            "#ffd700",
            "/party invite <name>  -  Invite a player",
            "/party kick <name>  -  Remove a member",
            "/party promote <name>  -  Transfer leadership",
            "/party disband  -  Disband the party"
        );

        showCard(cmd, 2, "+", "DUNGEON LIVES",
            "Dungeons use a shared life pool. Manage deaths carefully.",
            "#4aff7f",
            "Each party member contributes 3 lives",
            "Any death by any member costs 1 life",
            "The dungeon run ends when lives reach 0",
            "Larger parties have more lives but more risk",
            "Stay together and play carefully near bosses"
        );

        showCard(cmd, 3, "X", "FACTION RESTRICTIONS",
            "Faction allegiance determines who you can group with.",
            "#e74c3c",
            "Players in opposing factions cannot party together",
            "Neutral (unaligned) players can join anyone",
            "Guild members are always in the same faction",
            "Check your faction in /menu > Guild"
        );

        showCard(cmd, 4, "D", "FRACTURE GATES & DUNGEONS",
            "Instanced PvE content with scaling difficulty and valuable rewards.",
            "#9b59b6",
            "Fracture Gates open zombie survival arenas",
            "Difficulty scales with party size and level",
            "Battle waves of undead for gold and XP",
            "Bosses drop rare and legendary equipment",
            "Enter with a party for shared lives"
        );

        showCard(cmd, 5, "$", "LOOT & REWARDS",
            "How loot works in group content.",
            "#ff9f43",
            "Personal loot drops - each player gets their own",
            "Boss kills guarantee at least one rare+ item",
            "Legendary drops only from dungeon final bosses",
            "Gold rewards scale with wave completion",
            "Higher difficulty = better item level drops"
        );

        showStat(cmd, 0, "MAX SIZE", "8", "Players", "#4a9eff");
        showStat(cmd, 1, "LIVES", "3", "Per member", "#4aff7f");
        showStat(cmd, 2, "MAP", "Yes", "Shared pins", "#ffd700");
        showStat(cmd, 3, "LOOT", "Personal", "Not shared", "#8b5cf6");
    }

    private void renderFactionsGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Factions");

        showIntro(cmd, "The world is divided into factions at war. Every player "
            + "belongs to a faction and can join a guild within it. Guilds claim "
            + "territory, organize PvP, and compete on the leaderboards.");

        showCard(cmd, 0, "G", "GUILDS",
            "Guilds are player-run organizations within a faction. Create one or ask for an invite.",
            "#4a9eff",
            "Create via /menu > Guild with a name and 1-3 letter tag",
            "Guild tag appears before your name in chat",
            "Leaders can invite, promote, demote, and kick",
            "Officers share management responsibilities"
        );

        showCard(cmd, 1, "*", "GUILD RANKS",
            "Five roles define the guild hierarchy and permissions.",
            "#ffd700",
            "Leader  -  Full control, transfer ownership, disband",
            "Officer  -  Invite, kick, promote/demote, manage tag",
            "Senior  -  Trusted member, some management access",
            "Member  -  Standard access, guild chat, territory perks",
            "Recruit  -  New member, limited permissions"
        );

        showCard(cmd, 2, "T", "TERRITORY & CLAIMS",
            "Guilds can claim chunks of land for their faction.",
            "#4ecdc4",
            "Each guild has a claim limit based on size",
            "Claimed land shows your guild's banner",
            "Territory grants bonuses to guild members",
            "View claims in /menu > Guild > My Guild"
        );

        showCard(cmd, 3, "!", "FACTION WARFARE",
            "PvP between opposing factions earns Honor and controls territory.",
            "#e74c3c",
            "Kill enemy faction players for Honor points",
            "Higher-rank enemies give more Honor",
            "Territory control grants faction-wide bonuses",
            "Weekly rankings determine PvP brackets"
        );

        showCard(cmd, 4, "+", "GUILD BENEFITS",
            "Being in a guild provides tangible gameplay advantages.",
            "#4aff7f",
            "Private guild chat channel",
            "Shared territory with resource bonuses",
            "Organized group content and dungeon runs",
            "Faction leaderboard placement"
        );

        showCard(cmd, 5, "C", "GUILD MANAGEMENT PANEL",
            "All guild controls are available in /menu > Guild.",
            "#9b59b6",
            "Browse and search available guilds",
            "View member list with ranks and status",
            "Leaders: disband, promote, demote, kick",
            "Officers: invite players, manage guild tag",
            "Accept/decline incoming guild invitations"
        );

        showTip(cmd, "TIP: Open the Guild panel with /menu > Guild to browse guilds, "
            + "manage members, and view your faction's standings. Officers can invite "
            + "players and manage the guild tag.");
    }

    private void renderHonorGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Honor");

        showIntro(cmd, "The Honor system tracks PvP performance with weekly brackets "
            + "and a 14-rank progression. Earn Honor from kills, climb brackets for "
            + "Rank Points, and unlock exclusive rewards.");

        showStat(cmd, 0, "WEEKLY", "Honor", "Resets Sunday", "#ffd700");
        showStat(cmd, 1, "BRACKETS", "1-10", "By weekly rank", "#4aff7f");
        showStat(cmd, 2, "RANKS", "14", "Private to GM", "#8b5cf6");
        showStat(cmd, 3, "DECAY", "20%", "RP per week", "#e74c3c");

        showCard(cmd, 0, "H", "WEEKLY HONOR",
            "Honor is earned from PvP kills and resets every week.",
            "#ffd700",
            "Kill enemy faction players to earn Honor",
            "Honor amount scales with enemy rank",
            "Resets every Sunday at midnight UTC",
            "Your weekly total determines your bracket"
        );

        showCard(cmd, 1, "B", "BRACKET SYSTEM",
            "All PvP players are ranked into 10 brackets each week. Higher bracket = more RP.",
            "#4aff7f",
            "Bracket 1  (top, 15k+ honor)  -  +13,000 RP",
            "Bracket 3  (10k+ honor)  -  +9,000 RP",
            "Bracket 5  (6k+ honor)  -  +5,500 RP",
            "Bracket 7  (3k+ honor)  -  +3,000 RP",
            "Bracket 10 (1k+ honor)  -  +1,500 RP"
        );

        showCard(cmd, 2, "R", "RANK PROGRESSION",
            "14 ranks with increasing RP thresholds. Each rank grants a unique title.",
            "#8b5cf6",
            "Rank 1: Private  -  2,000 RP",
            "Rank 5: Sergeant Major  -  20,000 RP",
            "Rank 10: Lt. Commander  -  45,000 RP",
            "Rank 13: Field Marshal  -  60,000 RP",
            "Rank 14: Grand Marshal  -  65,000 RP"
        );

        showCard(cmd, 3, "D", "RANK POINT DECAY",
            "RP decays 20% weekly. You must PvP consistently to maintain rank.",
            "#e74c3c",
            "20% of your total RP is lost each week",
            "Bracket RP is added AFTER decay is applied",
            "Higher ranks require sustained weekly play",
            "Decay makes Grand Marshal extremely competitive"
        );

        showCard(cmd, 4, "$", "HONOR CURRENCY",
            "Separate from weekly Honor. A spendable currency earned from PvP.",
            "#ff9f43",
            "Earned from every PvP kill",
            "Does NOT reset weekly or decay",
            "Spend in the Honor Shop for gear and cosmetics",
            "Track balance in /menu > Honor"
        );

        showCard(cmd, 5, "S", "HONOR SHOP",
            "Spend your accumulated Honor on exclusive rewards at the Honor Shop.",
            "#d4af37",
            "Items have a rank requirement to purchase",
            "Higher-rank items are more powerful or rare",
            "Balance shown at top of shop page",
            "Locked items show required rank to buy",
            "Open via /menu > Honor > Honor Shop"
        );

        showTip(cmd, "TIP: Your bracket is based on your weekly Honor relative to ALL "
            + "other players. Consistent daily PvP is more effective than one big "
            + "session. Track your standing in /menu > Honor > My Standing.");
    }

    private void renderProfessionsGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Professions");

        showIntro(cmd, "Professions and Tradeskills let you craft items and gather "
            + "resources. Choose one main profession to specialize in for uncapped "
            + "leveling, and train six tradeskills by harvesting materials.");

        showCard(cmd, 0, "#", "CRAFTING PROFESSIONS (9)",
            "Each profession crafts a different category of items. Pick one as your main.",
            "#4ecdc4",
            "Bladesmith  -  Forge weapons and blades",
            "Platesmith  -  Craft plate armor and shields",
            "Alchemist  -  Brew potions and elixirs",
            "Cook  -  Prepare meals and feasts",
            "Leatherworker  -  Tan hides into leather gear",
            "Tailor  -  Weave cloth into fine garments"
        );

        showCard(cmd, 1, "+", "MORE PROFESSIONS",
            "Three additional crafting specializations.",
            "#4aff7f",
            "Carpenter  -  Build furniture and wooden tools",
            "Enchanter  -  Imbue items with arcane power",
            "Builder  -  Construct structures and fortifications",
            "Your main profession levels without limit",
            "Non-main professions are capped at a lower level"
        );

        showCard(cmd, 2, "T", "TRADESKILLS (6)",
            "Gathering skills that provide raw materials. Level by harvesting.",
            "#ffd700",
            "Mining  -  Ore and stone from rocks",
            "Woodcutting  -  Logs and planks from trees",
            "Farming  -  Crops and produce from soil",
            "Herbalism  -  Herbs and reagents from plants",
            "Skinning  -  Hides and leather from creatures",
            "Fishing  -  Fish and aquatic materials"
        );

        showCard(cmd, 3, "=", "LEVELING & PROGRESSION",
            "Both professions and tradeskills use the same XP curve.",
            "#8b5cf6",
            "Craft items at workbenches for profession XP",
            "Gather resources in the world for tradeskill XP",
            "Higher-tier recipes and nodes give more XP",
            "Each tradeskill level grants +1% bonus yield chance"
        );

        showCard(cmd, 4, "?", "TIPS & STRATEGY",
            "Make the most of the profession system.",
            "#9b59b6",
            "Pick a main profession that complements your class",
            "Level tradeskills passively while exploring",
            "MAIN badge in Skills panel marks your primary",
            "CAP badge shows when at non-main level cap",
            "Use /profession choose to set your main",
            "Check /menu > Skills for full progress"
        );

        showCard(cmd, 5, "W", "WORKBENCHES & CRAFTING",
            "Find the right workbench to craft items from your profession.",
            "#e67e22",
            "Each profession has dedicated workbenches",
            "Recipes unlock as your level increases",
            "Higher-level recipes require rarer materials",
            "Crafting grants XP toward your profession level",
            "Failed crafts still grant partial XP"
        );

        showStat(cmd, 0, "PROFESSIONS", "9", "Total types", "#4ecdc4");
        showStat(cmd, 1, "TRADESKILLS", "6", "Gathering", "#ffd700");
        showStat(cmd, 2, "MAIN", "1", "Uncapped", "#4aff7f");
        showStat(cmd, 3, "YIELD", "+1%", "Per level", "#ff9f43");

        showTip(cmd, "TIP: Pair a gathering tradeskill with a crafting profession. "
            + "For example, Mining + Bladesmith or Herbalism + Alchemist. "
            + "Track all your skills in /menu > Skills.");
    }

    private void renderRecruitmentGuide(UICommandBuilder cmd) {
        showHeaderImage(cmd, "Recruitment");

        showIntro(cmd, "The Recruitment system lets guilds compete to recruit strong "
            + "players. Complete the Asylum challenge to earn a rank, then receive "
            + "gold bids from guilds during timed bidding windows.");

        showCard(cmd, 0, "!", "THE ASYLUM CHALLENGE",
            "To become a recruit, you must prove yourself by defeating the Asylum boss.",
            "#e74c3c",
            "Enter the Asylum dungeon solo or with a party",
            "Defeat the boss to earn a recruit rank",
            "Your rank is based on damage % dealt to the boss",
            "Higher damage % = higher rank tier",
            "Boss max HP is recorded for your rank calculation"
        );

        showCard(cmd, 1, "*", "RANK TIERS",
            "Recruit ranks are color-coded by performance tier.",
            "#ffd700",
            "S-Rank (Gold)  -  Top-tier damage dealers",
            "A-Rank (Purple)  -  Strong performance",
            "B-Rank (Blue)  -  Above-average contribution",
            "C-Rank (Green)  -  Solid effort, boss defeated",
            "D-Rank (Gray)  -  Participated but low damage"
        );

        showCard(cmd, 2, "B", "BIDDING SYSTEM",
            "Guilds place gold bids on recruits during timed bidding windows.",
            "#4a9eff",
            "Bidding windows open periodically per recruit",
            "Multiple guilds can bid on the same player",
            "Recruits see all bids with guild name and amount",
            "Accepted bids auto-invite the recruit to the guild",
            "Declined bids refund the gold to the guild bank"
        );

        showCard(cmd, 3, "G", "GUILD OFFICERS: BIDDING",
            "Officers browse recruits and place bids on behalf of the guild.",
            "#4ecdc4",
            "Browse Recruits  -  See all ranked players",
            "\"OPEN\" status = bidding window is active",
            "Place bids with guild bank gold",
            "Track outgoing bids in Guild Bids tab",
            "Withdraw bids before the recruit decides"
        );

        showCard(cmd, 4, "$", "FOR RECRUITS: BIDS ON ME",
            "As a recruit, manage incoming bids from guilds.",
            "#ff9f43",
            "View all bids with guild name, tag, and amount",
            "See who placed the bid (officer name)",
            "Accept to join that guild immediately",
            "Decline to remove the bid and refund gold",
            "Check /menu > Recruitment > Bids on Me"
        );

        showCard(cmd, 5, "?", "GETTING STARTED",
            "Step-by-step guide to using the recruitment system.",
            "#9b59b6",
            "1. Complete the Asylum dungeon to earn a rank",
            "2. Wait for your bidding window to open",
            "3. Review incoming bids in /menu > Recruitment",
            "4. Accept a bid to join that guild",
            "5. Your rank persists even after joining"
        );

        showStat(cmd, 0, "RANK", "Permanent", "Once earned", "#ffd700");
        showStat(cmd, 1, "BIDS", "Gold", "Escrowed", "#ff9f43");
        showStat(cmd, 2, "WINDOW", "Timed", "Per recruit", "#4a9eff");
        showStat(cmd, 3, "PANEL", "/menu", "Recruitment", "#4ecdc4");

        showTip(cmd, "TIP: Your Asylum rank is permanent and carries over even after "
            + "joining a guild. Higher ranks attract better bids. Challenge the Asylum "
            + "boss multiple times to improve your ranking.");
    }
}
