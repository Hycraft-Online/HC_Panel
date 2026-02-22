package com.hcpanel.gui.content;

import com.hcpanel.gui.UnifiedPanelGui.SidebarButton;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

// Direct imports from HC_Factions (optional dependency)
import com.hcfactions.HC_FactionsPlugin;
import com.hcfactions.models.Faction;
import com.hcfactions.models.Guild;
import com.hcfactions.models.GuildInvitation;
import com.hcfactions.models.GuildRole;
import com.hcfactions.models.PlayerData;

import com.hypixel.hytale.server.core.universe.Universe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Renders Factions module content into the unified panel.
 */
public class FactionsContentRenderer {

    private final PlayerRef playerRef;
    private String browserSearch;

    public FactionsContentRenderer(PlayerRef playerRef) {
        this.playerRef = playerRef;
    }

    public void setBrowserSearch(String search) {
        this.browserSearch = search;
    }

    // ========== Plugin access ==========

    private HC_FactionsPlugin getPlugin() {
        return HC_FactionsPlugin.getInstance();
    }

    private boolean isFactionsAvailable() {
        return getPlugin() != null;
    }

    // ========== Sidebar ==========

    public List<SidebarButton> getSidebarButtons(String currentView) {
        List<SidebarButton> buttons = new ArrayList<>();

        boolean isGuild = currentView == null || "guild".equals(currentView);
        boolean isMembers = "members".equals(currentView);
        boolean isBrowser = "browser".equals(currentView);
        boolean isInvitations = "invitations".equals(currentView);
        boolean isOverview = "overview".equals(currentView);

        // Always show guild option
        buttons.add(new SidebarButton("My Guild", "nav:factions:guild", null, "#4a9eff", isGuild));

        // Show manage members for officers+
        if (isOfficerOrHigher()) {
            buttons.add(new SidebarButton("Members", "nav:factions:members", null, "#4a9eff", isMembers));
        }

        // Always show browser
        buttons.add(new SidebarButton("Browse Guilds", "nav:factions:browser", null, "#4a9eff", isBrowser));

        // Show invitations with badge
        String inviteBadge = getInvitationBadge();
        buttons.add(new SidebarButton("Invitations", "nav:factions:invitations", inviteBadge, "#4a9eff", isInvitations));

        // Faction overview
        buttons.add(new SidebarButton("Faction Overview", "nav:factions:overview", null, "#4a9eff", isOverview));

        return buttons;
    }

    public String getInvitationBadge() {
        HC_FactionsPlugin plugin = getPlugin();
        if (plugin == null) return null;

        int count = plugin.getGuildManager().getPendingInvitationCount(playerRef.getUuid());
        return count > 0 ? String.valueOf(count) : null;
    }

    /**
     * Returns the faction tag for the player.
     */
    public String getFactionTag() {
        Faction faction = resolveFaction();
        return faction != null ? faction.getShortName() : null;
    }

    // ========== Helpers ==========

    private Faction resolveFaction() {
        HC_FactionsPlugin plugin = getPlugin();
        if (plugin == null) return null;
        PlayerData pd = plugin.getPlayerDataRepository().getPlayerData(playerRef.getUuid());
        if (pd == null || pd.getFactionId() == null) return null;
        return plugin.getFactionManager().getFaction(pd.getFactionId());
    }

    private Guild resolveGuild() {
        HC_FactionsPlugin plugin = getPlugin();
        if (plugin == null) return null;
        PlayerData pd = plugin.getPlayerDataRepository().getPlayerData(playerRef.getUuid());
        if (pd == null || !pd.isInGuild()) return null;
        return plugin.getGuildManager().getGuild(pd.getGuildId());
    }

    private PlayerData resolvePlayerData() {
        HC_FactionsPlugin plugin = getPlugin();
        if (plugin == null) return null;
        return plugin.getPlayerDataRepository().getPlayerData(playerRef.getUuid());
    }

    private String getRoleColor(GuildRole role) {
        if (role == null) return "#ffffff";
        return switch (role) {
            case LEADER -> "#FFD700";
            case OFFICER -> "#4FC3F7";
            case SENIOR -> "#66BB6A";
            case MEMBER -> "#81C784";
            case RECRUIT -> "#BDBDBD";
        };
    }

    private boolean isOfficerOrHigher() {
        PlayerData playerData = resolvePlayerData();
        if (playerData == null) return false;
        GuildRole role = playerData.getGuildRole();
        return role != null && role.hasAtLeast(GuildRole.OFFICER);
    }

    private boolean canManageMember(GuildRole myRole, GuildRole targetRole) {
        if (myRole == null || targetRole == null) return false;
        if (targetRole == GuildRole.LEADER) return false;
        if (myRole == GuildRole.LEADER) return true;
        if (myRole == GuildRole.OFFICER) {
            return targetRole == GuildRole.MEMBER || targetRole == GuildRole.RECRUIT;
        }
        return false;
    }

    // ========== Content rendering ==========

    public void renderContent(UICommandBuilder cmd, UIEventBuilder events, Store<EntityStore> store,
                               PlayerRef playerRef, String view, String subView) {
        if (!isFactionsAvailable()) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "Factions system not available.");
            return;
        }

        if (view == null) view = "guild";

        switch (view) {
            case "guild" -> renderGuild(cmd, events);
            case "members" -> renderMembers(cmd, events, subView);
            case "browser" -> renderBrowser(cmd, events, subView);
            case "invitations" -> renderInvitations(cmd, events, subView);
            case "overview" -> renderOverview(cmd, events);
            default -> renderGuild(cmd, events);
        }
    }

    private void renderGuild(UICommandBuilder cmd, UIEventBuilder events) {
        Guild guild = resolveGuild();
        if (guild == null) {
            renderNoGuild(cmd, events);
            return;
        }

        HC_FactionsPlugin plugin = getPlugin();
        PlayerData playerData = resolvePlayerData();
        Faction faction = resolveFaction();



        // Stats section
        cmd.set("#FactionStatsSection.Visible", true);

        int power = guild.getPower();
        cmd.set("#PowerValue.Text", String.valueOf(power));
        cmd.set("#PowerDesc.Text", "claim strength");

        GuildRole guildRole = playerData != null ? playerData.getGuildRole() : null;
        String roleName = guildRole != null ? guildRole.getDisplayName() : "Member";
        cmd.set("#RankValue.Text", roleName);
        cmd.set("#RankValue.Style.TextColor", getRoleColor(guildRole));

        List<PlayerData> members = plugin.getGuildManager().getGuildMembers(guild.getId());
        int memberCount = members != null ? members.size() : 0;
        int maxMembers = plugin.getConfig().getGuildMaxMembers();
        cmd.set("#MemberCountValue.Text", memberCount + "/" + maxMembers);
        cmd.set("#MembersDesc.Text", "guild members");

        // Extended stats
        cmd.set("#GuildExtendedStats.Visible", true);

        int claimCount = plugin.getClaimManager().getClaimCount(guild.getId());
        int maxClaims = plugin.getClaimManager().getMaxClaims(guild.getId());
        cmd.set("#ClaimsValue.Text", claimCount + "/" + maxClaims);

        // Progress bar: Anchor width out of 318px (card inner width)
        // Layout: 960 panel - 200 sidebar = 760 right panel - 40 padding = 720
        // Two cards FlexWeight:1 with 12px gap = (720-12)/2 = 354 - 36 padding = 318
        int maxBarWidth = 318;
        int barWidth;
        if (claimCount > maxClaims) {
            cmd.set("#ClaimsDesc.Text", "over limit!");
            cmd.set("#ClaimsValue.Style.TextColor", "#ff6b6b");
            cmd.set("#ClaimsProgressBar.Background", "#ff6b6b");
            barWidth = maxBarWidth;
        } else if (claimCount >= maxClaims && maxClaims > 0) {
            cmd.set("#ClaimsDesc.Text", "fully claimed");
            cmd.set("#ClaimsValue.Style.TextColor", "#FFD700");
            cmd.set("#ClaimsProgressBar.Background", "#FFD700");
            barWidth = maxBarWidth;
        } else if (maxClaims > 0 && claimCount > 0) {
            cmd.set("#ClaimsDesc.Text", "chunks claimed");
            cmd.set("#ClaimsProgressBar.Background", "#9CCC65");
            barWidth = Math.max(1, (int)((double) maxBarWidth * claimCount / maxClaims));
        } else {
            cmd.set("#ClaimsDesc.Text", "chunks claimed");
            cmd.set("#ClaimsProgressBar.Background", "#9CCC65");
            barWidth = 0;
        }
        com.hypixel.hytale.server.core.ui.Anchor claimsBarAnchor = new com.hypixel.hytale.server.core.ui.Anchor();
        claimsBarAnchor.setWidth(com.hypixel.hytale.server.core.ui.Value.of(barWidth));
        claimsBarAnchor.setHeight(com.hypixel.hytale.server.core.ui.Value.of(8));
        cmd.setObject("#ClaimsProgressBar.Anchor", claimsBarAnchor);

        int powerPerMember = plugin.getConfig().getGuildDefaultPower();
        cmd.set("#PowerFormula.Text", "Base: " + powerPerMember + " per member");
        cmd.set("#PowerPerMember.Text", "Total: " + power + " (" + memberCount + " members)");
        cmd.set("#MaxClaimsInfo.Text", "Max claims: " + maxClaims);

        // Tag editor (officers+)
        if (guildRole != null && guildRole.hasAtLeast(GuildRole.OFFICER)) {
            cmd.set("#GuildTagSection.Visible", true);

            // Pre-fill the input with current tag
            String currentTag = guild.getTag();
            if (currentTag != null && !currentTag.isEmpty()) {
                cmd.set("#GuildTagInput.Value", currentTag);
            }

            events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#GuildTagInput",
                EventData.of("@GuildTagInput", "#GuildTagInput.Value"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#SetTagBtn",
                EventData.of("Action", "action:set_tag:submit"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#ClearTagBtn",
                EventData.of("Action", "action:clear_tag:submit"), false);
        }

        // Quick info
        cmd.set("#FactionQuickInfo.Visible", true);
        String factionName = faction != null ? faction.getDisplayName() : "Unknown";
        cmd.set("#QuickInfoText.Text",
            factionName + " -- Power drives land claims. Recruit members to grow stronger.");

        // Leave guild (non-leaders)
        if (guildRole != GuildRole.LEADER) {
            cmd.set("#GuildActionsSection.Visible", true);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#LeaveGuildBtn",
                EventData.of("Action", "action:leave_guild:confirm"), false);
        }

        cmd.set("#FooterText.Text", "Use /guild for more options.");
    }

    private void renderNoGuild(UICommandBuilder cmd, UIEventBuilder events) {
        Faction faction = resolveFaction();


        // Create guild section (only if player has a faction)
        if (faction != null) {
            cmd.set("#CreateGuildSection.Visible", true);

            events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#GuildNameInput",
                EventData.of("@GuildNameInput", "#GuildNameInput.Value"), false);
            events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#CreateGuildTagInput",
                EventData.of("@GuildTagInput", "#CreateGuildTagInput.Value"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#CreateGuildBtn",
                EventData.of("Action", "action:create_guild:submit"), false);
        }

        cmd.set("#FactionQuickInfo.Visible", true);
        cmd.set("#QuickInfoText.Text",
            "You are not in a guild.\n\n" +
            "Options:\n" +
            "- Create your own guild using the form above\n" +
            "- Browse existing guilds to request to join\n" +
            "- Check your invitations for pending offers");

        cmd.set("#FooterText.Text", "Join a guild to access land claims and group features.");
    }

    private static final int MEMBERS_PER_PAGE = 20;

    private void renderMembers(UICommandBuilder cmd, UIEventBuilder events, String pageParam) {
        Guild guild = resolveGuild();
        if (guild == null) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "You must be in a guild to view members.");
            return;
        }

        HC_FactionsPlugin plugin = getPlugin();
        Faction faction = resolveFaction();
        PlayerData myData = resolvePlayerData();
        GuildRole myRole = myData != null ? myData.getGuildRole() : null;



        // Invite section (officers+)
        if (myRole != null && myRole.hasAtLeast(GuildRole.OFFICER)) {
            cmd.set("#InvitePlayerSection.Visible", true);
            events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#InvitePlayerInput",
                EventData.of("@InvitePlayerInput", "#InvitePlayerInput.Value"), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#InvitePlayerBtn",
                EventData.of("Action", "action:invite_player:submit"), false);
        }

        // Member list
        cmd.set("#MemberListSection.Visible", true);
        List<PlayerData> members = plugin.getGuildManager().getGuildMembers(guild.getId());
        int totalMembers = members.size();
        int totalPages = Math.max(1, (totalMembers + MEMBERS_PER_PAGE - 1) / MEMBERS_PER_PAGE);

        // Parse current page
        int currentPage = 1;
        if (pageParam != null) {
            try {
                currentPage = Integer.parseInt(pageParam);
            } catch (NumberFormatException ignored) {}
        }
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        int startIndex = (currentPage - 1) * MEMBERS_PER_PAGE;
        int endIndex = Math.min(startIndex + MEMBERS_PER_PAGE, totalMembers);

        // Build set of online player UUIDs
        Set<UUID> onlineUuids = new HashSet<>();
        for (PlayerRef onlinePlayer : Universe.get().getPlayers()) {
            onlineUuids.add(onlinePlayer.getUuid());
        }

        int rowIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            PlayerData member = members.get(i);
            UUID memberUuid = member.getPlayerUuid();
            GuildRole memberRole = member.getGuildRole();
            String roleColor = getRoleColor(memberRole);

            cmd.set("#MemberRow" + rowIndex + ".Visible", true);
            cmd.set("#MemberName" + rowIndex + ".Text", member.getPlayerName());
            cmd.set("#MemberRole" + rowIndex + ".Text",
                memberRole != null ? "[" + memberRole.getDisplayName() + "]" : "[Member]");
            cmd.set("#MemberRole" + rowIndex + ".Style.TextColor", roleColor);

            // Online status indicator
            boolean isOnline = onlineUuids.contains(memberUuid);
            cmd.set("#OnlineIndicator" + rowIndex + ".Background", isOnline ? "#4aff7f" : "#757575");

            boolean isSelf = memberUuid.equals(playerRef.getUuid());
            boolean canManage = myRole != null && !isSelf && canManageMember(myRole, memberRole);

            if (canManage) {
                String memberIdStr = memberUuid.toString();

                boolean canPromote = memberRole != null && memberRole != GuildRole.LEADER &&
                    (myRole == GuildRole.LEADER || memberRole.ordinal() > myRole.ordinal() + 1);
                if (canPromote) {
                    cmd.set("#MemberPromote" + rowIndex + ".Visible", true);
                    events.addEventBinding(CustomUIEventBindingType.Activating, "#MemberPromote" + rowIndex,
                        EventData.of("Action", "action:promote:" + memberIdStr), false);
                }

                boolean canDemote = memberRole != null && memberRole != GuildRole.RECRUIT &&
                    memberRole != GuildRole.LEADER;
                if (canDemote) {
                    cmd.set("#MemberDemote" + rowIndex + ".Visible", true);
                    events.addEventBinding(CustomUIEventBindingType.Activating, "#MemberDemote" + rowIndex,
                        EventData.of("Action", "action:demote:" + memberIdStr), false);
                }

                cmd.set("#MemberKick" + rowIndex + ".Visible", true);
                events.addEventBinding(CustomUIEventBindingType.Activating, "#MemberKick" + rowIndex,
                    EventData.of("Action", "action:kick:" + memberIdStr), false);
            }

            rowIndex++;
        }

        for (int i = rowIndex; i < MEMBERS_PER_PAGE; i++) {
            cmd.set("#MemberRow" + i + ".Visible", false);
        }

        // Pagination controls
        if (totalPages > 1) {
            cmd.set("#MemberPagination.Visible", true);
            cmd.set("#MemberPageInfo.Text", "Page " + currentPage + " of " + totalPages);

            if (currentPage > 1) {
                cmd.set("#MemberPrevPage.Visible", true);
                events.addEventBinding(CustomUIEventBindingType.Activating, "#MemberPrevPage",
                    EventData.of("Action", "nav:factions:members:" + (currentPage - 1)), false);
            }

            if (currentPage < totalPages) {
                cmd.set("#MemberNextPage.Visible", true);
                events.addEventBinding(CustomUIEventBindingType.Activating, "#MemberNextPage",
                    EventData.of("Action", "nav:factions:members:" + (currentPage + 1)), false);
            }
        }

        cmd.set("#FooterText.Text", totalMembers + " member" + (totalMembers != 1 ? "s" : "") + " in guild"
            + (totalPages > 1 ? " (showing " + (startIndex + 1) + "-" + endIndex + ")" : ""));
    }

    private static final int GUILDS_PER_PAGE = 6;

    private void renderBrowser(UICommandBuilder cmd, UIEventBuilder events, String pageParam) {
        HC_FactionsPlugin plugin = getPlugin();
        PlayerData playerData = resolvePlayerData();
        Faction faction = resolveFaction();



        String playerFactionId = playerData != null ? playerData.getFactionId() : null;
        if (playerFactionId == null) {
            cmd.set("#FactionQuickInfo.Visible", true);
            cmd.set("#QuickInfoText.Text", "You must select a faction before browsing guilds.\n\nUse /faction to choose your faction.");
            cmd.set("#FooterText.Text", "Select a faction first.");
            return;
        }

        cmd.set("#GuildListSection.Visible", true);
        boolean playerInGuild = playerData.isInGuild();
        List<Guild> guilds = new ArrayList<>(plugin.getGuildManager().getGuildsByFaction(playerFactionId));

        // Sort by power descending, then alphabetically by name
        guilds.sort((a, b) -> {
            int powerCmp = Integer.compare(b.getPower(), a.getPower());
            if (powerCmp != 0) return powerCmp;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        // Search bar bindings
        events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#BrowserSearchInput",
            EventData.of("@BrowserSearchInput", "#BrowserSearchInput.Value"), false);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#BrowserSearchBtn",
            EventData.of("Action", "action:browser_search:submit")
                .append("@BrowserSearchInput", "#BrowserSearchInput.Value"), false);

        // Pre-fill search if present
        if (browserSearch != null && !browserSearch.isBlank()) {
            cmd.set("#BrowserSearchInput.Value", browserSearch);
            String searchLower = browserSearch.toLowerCase();
            guilds.removeIf(g -> !g.getName().toLowerCase().contains(searchLower));
        }

        int totalGuilds = guilds.size();
        int totalPages = Math.max(1, (totalGuilds + GUILDS_PER_PAGE - 1) / GUILDS_PER_PAGE);

        // Parse current page
        int currentPage = 1;
        if (pageParam != null) {
            try {
                currentPage = Integer.parseInt(pageParam);
            } catch (NumberFormatException ignored) {}
        }
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        int startIndex = (currentPage - 1) * GUILDS_PER_PAGE;
        int endIndex = Math.min(startIndex + GUILDS_PER_PAGE, totalGuilds);

        int rowIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Guild guildObj = guilds.get(i);

            List<PlayerData> members = plugin.getGuildManager().getGuildMembers(guildObj.getId());
            int memberCount = members != null ? members.size() : 0;
            int maxMembers = plugin.getConfig().getGuildMaxMembers();

            cmd.set("#GuildRow" + rowIndex + ".Visible", true);
            cmd.set("#GuildName" + rowIndex + ".Text", guildObj.getName());
            cmd.set("#GuildMembers" + rowIndex + ".Text", memberCount + "/" + maxMembers);
            cmd.set("#GuildPower" + rowIndex + ".Text", guildObj.getPower() + " pwr");

            if (!playerInGuild) {
                cmd.set("#GuildJoin" + rowIndex + ".Visible", true);
                cmd.set("#GuildJoinDisabled" + rowIndex + ".Visible", false);
                events.addEventBinding(CustomUIEventBindingType.Activating, "#GuildJoin" + rowIndex,
                    EventData.of("Action", "action:guild_join:" + guildObj.getId().toString()), false);
            } else {
                cmd.set("#GuildJoin" + rowIndex + ".Visible", false);
                cmd.set("#GuildJoinDisabled" + rowIndex + ".Visible", true);
            }

            rowIndex++;
        }

        for (int i = rowIndex; i < GUILDS_PER_PAGE; i++) {
            cmd.set("#GuildRow" + i + ".Visible", false);
        }

        if (totalGuilds == 0) {
            cmd.set("#GuildListSection.Visible", false);
            cmd.set("#FactionQuickInfo.Visible", true);
            if (browserSearch != null && !browserSearch.isBlank()) {
                cmd.set("#QuickInfoText.Text", "No guilds match your search.\n\nTry a different search term or clear the search.");
            } else {
                cmd.set("#QuickInfoText.Text", "No guilds found for your faction.\n\nBe the first to create one using /guild create <name>");
            }
        }

        // Pagination controls
        if (totalPages > 1) {
            cmd.set("#GuildPagination.Visible", true);
            cmd.set("#GuildPageInfo.Text", "Page " + currentPage + " of " + totalPages);

            if (currentPage > 1) {
                cmd.set("#GuildPrevPage.Visible", true);
                events.addEventBinding(CustomUIEventBindingType.Activating, "#GuildPrevPage",
                    EventData.of("Action", "nav:factions:browser:" + (currentPage - 1)), false);
            }

            if (currentPage < totalPages) {
                cmd.set("#GuildNextPage.Visible", true);
                events.addEventBinding(CustomUIEventBindingType.Activating, "#GuildNextPage",
                    EventData.of("Action", "nav:factions:browser:" + (currentPage + 1)), false);
            }
        }

        String footerBase = playerInGuild
            ? "Leave your current guild to join another."
            : "Click JOIN to request membership.";
        cmd.set("#FooterText.Text", totalGuilds + " guild" + (totalGuilds != 1 ? "s" : "")
            + (totalPages > 1 ? " (showing " + (startIndex + 1) + "-" + endIndex + ") - " : " - ") + footerBase);
    }

    private static final int INVITES_PER_PAGE = 5;

    private void renderInvitations(UICommandBuilder cmd, UIEventBuilder events, String pageParam) {
        HC_FactionsPlugin plugin = getPlugin();
        Faction faction = resolveFaction();

        List<GuildInvitation> invitations = plugin.getGuildManager().getPendingInvitations(playerRef.getUuid());
        int totalInvites = invitations.size();

        if (totalInvites == 0) {
            cmd.set("#FactionQuickInfo.Visible", true);
            cmd.set("#QuickInfoText.Text",
                "No Pending Invitations\n\n" +
                "Browse guilds or ask a guild officer to invite you.\n" +
                "New invitations will appear here automatically.");
            cmd.set("#FooterText.Text", "");
            return;
        }

        cmd.set("#InviteListSection.Visible", true);

        int totalPages = Math.max(1, (totalInvites + INVITES_PER_PAGE - 1) / INVITES_PER_PAGE);

        // Parse current page
        int currentPage = 1;
        if (pageParam != null) {
            try {
                currentPage = Integer.parseInt(pageParam);
            } catch (NumberFormatException ignored) {}
        }
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        int startIndex = (currentPage - 1) * INVITES_PER_PAGE;
        int endIndex = Math.min(startIndex + INVITES_PER_PAGE, totalInvites);

        int rowIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            GuildInvitation invitation = invitations.get(i);
            UUID guildId = invitation.getGuildId();
            Guild guildObj = plugin.getGuildManager().getGuild(guildId);
            String guildName = guildObj != null ? guildObj.getName() : "Unknown Guild";

            cmd.set("#InviteRow" + rowIndex + ".Visible", true);
            cmd.set("#InviteGuildName" + rowIndex + ".Text", guildName);
            cmd.set("#InviteFrom" + rowIndex + ".Text", "from " + invitation.getInviterName());

            String guildIdStr = guildId.toString();
            events.addEventBinding(CustomUIEventBindingType.Activating, "#InviteAccept" + rowIndex,
                EventData.of("Action", "action:invite_accept:" + guildIdStr), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#InviteDecline" + rowIndex,
                EventData.of("Action", "action:invite_decline:" + guildIdStr), false);

            rowIndex++;
        }

        for (int i = rowIndex; i < INVITES_PER_PAGE; i++) {
            cmd.set("#InviteRow" + i + ".Visible", false);
        }

        // Pagination controls
        if (totalPages > 1) {
            cmd.set("#InvitePagination.Visible", true);
            cmd.set("#InvitePageInfo.Text", "Page " + currentPage + " of " + totalPages);

            if (currentPage > 1) {
                cmd.set("#InvitePrevPage.Visible", true);
                events.addEventBinding(CustomUIEventBindingType.Activating, "#InvitePrevPage",
                    EventData.of("Action", "nav:factions:invitations:" + (currentPage - 1)), false);
            }

            if (currentPage < totalPages) {
                cmd.set("#InviteNextPage.Visible", true);
                events.addEventBinding(CustomUIEventBindingType.Activating, "#InviteNextPage",
                    EventData.of("Action", "nav:factions:invitations:" + (currentPage + 1)), false);
            }
        }

        cmd.set("#FooterText.Text", totalInvites + " invitation" + (totalInvites != 1 ? "s" : "")
            + (totalPages > 1 ? " (showing " + (startIndex + 1) + "-" + endIndex + ") - " : " - ")
            + "Click ACCEPT or DECLINE to respond.");
    }

    private void renderOverview(UICommandBuilder cmd, UIEventBuilder events) {
        Faction faction = resolveFaction();
        if (faction == null) {
            cmd.set("#FactionQuickInfo.Visible", true);
            cmd.set("#QuickInfoText.Text", "You must select a faction first.\n\nUse /faction to choose your faction.");
            cmd.set("#FooterText.Text", "Select a faction to view its overview.");
            return;
        }

        HC_FactionsPlugin plugin = getPlugin();


        // Faction-wide stats
        String factionId = faction.getId();
        List<Guild> guilds = plugin.getGuildManager().getGuildsByFaction(factionId);
        int guildCount = guilds != null ? guilds.size() : 0;

        int totalPower = 0;
        int totalMembers = 0;
        if (guilds != null) {
            for (Guild g : guilds) {
                totalPower += g.getPower();
                List<PlayerData> members = plugin.getGuildManager().getGuildMembers(g.getId());
                totalMembers += members != null ? members.size() : 0;
            }
        }

        cmd.set("#FactionStatsSection.Visible", true);
        cmd.set("#PowerValue.Text", String.valueOf(totalPower));
        cmd.set("#PowerDesc.Text", "combined power");
        cmd.set("#PowerDesc.Style.TextColor", "#96a9be");
        cmd.set("#RankValue.Text", String.valueOf(guildCount));
        cmd.set("#RankDesc.Text", "total guilds");
        cmd.set("#RankDesc.Style.TextColor", "#96a9be");
        cmd.set("#MemberCountValue.Text", String.valueOf(totalMembers));
        cmd.set("#MembersDesc.Text", "total players");
        cmd.set("#MembersDesc.Style.TextColor", "#96a9be");

        // Top 3 guilds leaderboard (sorted by power)
        if (guilds != null && !guilds.isEmpty()) {
            guilds.sort((a, b) -> Integer.compare(b.getPower(), a.getPower()));
            cmd.set("#TopGuildsSection.Visible", true);

            int topCount = Math.min(3, guilds.size());
            for (int i = 0; i < topCount; i++) {
                Guild g = guilds.get(i);
                List<PlayerData> gMembers = plugin.getGuildManager().getGuildMembers(g.getId());
                int gMemberCount = gMembers != null ? gMembers.size() : 0;

                cmd.set("#TopGuildRow" + i + ".Visible", true);
                cmd.set("#TopGuildName" + i + ".Text", g.getName());
                cmd.set("#TopGuildPower" + i + ".Text", g.getPower() + " pwr");
                cmd.set("#TopGuildMembers" + i + ".Text", gMemberCount + " members");
            }
        }

        cmd.set("#FactionQuickInfo.Visible", true);
        cmd.set("#QuickInfoText.Text",
            "Your faction competes against others for territory and resources.\n" +
            "Support your guild and faction by participating in PvP combat!");

        cmd.set("#FooterText.Text", "Fight for your faction's honor in PvP combat!");
    }

    // ========== Action handling ==========

    public boolean handleGuildAction(PlayerRef playerRef, String action) {
        return handleGuildAction(playerRef, action, null, null, null);
    }

    public boolean handleGuildAction(PlayerRef playerRef, String action,
                                      String guildNameInput, String guildTagInput, String invitePlayerInput) {
        HC_FactionsPlugin plugin = getPlugin();
        if (plugin == null) return false;

        String[] parts = action.split(":");
        if (parts.length < 2) return false;

        String actionType = parts[0];
        String param = parts[1];

        return switch (actionType) {
            case "guild_join" -> {
                UUID guildId = UUID.fromString(param);
                Guild guild = plugin.getGuildManager().getGuild(guildId);
                yield guild != null && plugin.getGuildManager().createJoinRequest(guildId, playerRef.getUuid());
            }
            case "invite_accept" -> plugin.getGuildManager().joinGuild(UUID.fromString(param), playerRef.getUuid());
            case "invite_decline" -> {
                plugin.getGuildManager().removeInvitation(UUID.fromString(param), playerRef.getUuid());
                yield true;
            }
            case "leave_guild" -> plugin.getGuildManager().leaveGuild(playerRef.getUuid());
            case "create_guild" -> handleCreateGuild(plugin, guildNameInput, guildTagInput);
            case "promote" -> handleRoleChange(plugin, param, playerRef, (gm, gid, tid, uid) -> gm.promotePlayer(gid, tid, uid));
            case "demote" -> handleRoleChange(plugin, param, playerRef, (gm, gid, tid, uid) -> gm.demotePlayer(gid, tid, uid));
            case "kick" -> handleRoleChange(plugin, param, playerRef, (gm, gid, tid, uid) -> gm.kickPlayer(gid, tid, uid));
            case "set_tag" -> handleSetTag(plugin, guildTagInput);
            case "clear_tag" -> handleClearTag(plugin);
            case "invite_player" -> handleInvitePlayer(plugin, invitePlayerInput);
            default -> false;
        };
    }

    @FunctionalInterface
    private interface RoleAction {
        boolean apply(com.hcfactions.managers.GuildManager gm, UUID guildId, UUID targetUuid, UUID actorUuid);
    }

    private boolean handleRoleChange(HC_FactionsPlugin plugin, String param, PlayerRef actor, RoleAction action) {
        UUID targetUuid = UUID.fromString(param);
        PlayerData playerData = plugin.getPlayerDataRepository().getPlayerData(actor.getUuid());
        if (playerData == null || !playerData.isInGuild()) return false;
        return action.apply(plugin.getGuildManager(), playerData.getGuildId(), targetUuid, actor.getUuid());
    }

    private boolean handleCreateGuild(HC_FactionsPlugin plugin, String guildNameInput, String guildTagInput) {
        if (guildNameInput == null || guildNameInput.isBlank()) return false;
        PlayerData playerData = plugin.getPlayerDataRepository().getPlayerData(playerRef.getUuid());
        if (playerData == null || playerData.getFactionId() == null) return false;

        String tag = null;
        if (guildTagInput != null && !guildTagInput.isBlank()) {
            tag = guildTagInput.trim().toUpperCase();
            if (tag.length() > 3 || !tag.matches("[A-Z]+")) return false;
        }

        Guild created = plugin.getGuildManager().createGuild(
            guildNameInput.trim(), playerRef.getUuid(), playerData.getFactionId(), tag);
        return created != null;
    }

    private boolean handleSetTag(HC_FactionsPlugin plugin, String guildTagInput) {
        if (guildTagInput == null || guildTagInput.isBlank()) return false;
        String newTag = guildTagInput.trim().toUpperCase();
        if (!newTag.matches("^[A-Z]{1,3}$")) return false;

        PlayerData playerData = plugin.getPlayerDataRepository().getPlayerData(playerRef.getUuid());
        if (playerData == null || !playerData.isInGuild()) return false;
        if (playerData.getGuildRole() == null || !playerData.getGuildRole().hasAtLeast(GuildRole.OFFICER)) return false;

        Guild guild = plugin.getGuildManager().getGuild(playerData.getGuildId());
        if (guild == null) return false;
        guild.setTag(newTag);
        plugin.getGuildRepository().updateGuild(guild);
        return true;
    }

    private boolean handleClearTag(HC_FactionsPlugin plugin) {
        PlayerData playerData = plugin.getPlayerDataRepository().getPlayerData(playerRef.getUuid());
        if (playerData == null || !playerData.isInGuild()) return false;
        if (playerData.getGuildRole() == null || !playerData.getGuildRole().hasAtLeast(GuildRole.OFFICER)) return false;

        Guild guild = plugin.getGuildManager().getGuild(playerData.getGuildId());
        if (guild == null) return false;
        guild.setTag(null);
        plugin.getGuildRepository().updateGuild(guild);
        return true;
    }

    private boolean handleInvitePlayer(HC_FactionsPlugin plugin, String invitePlayerInput) {
        if (invitePlayerInput == null || invitePlayerInput.isBlank()) return false;

        PlayerData playerData = plugin.getPlayerDataRepository().getPlayerData(playerRef.getUuid());
        if (playerData == null || !playerData.isInGuild()) return false;
        if (playerData.getGuildRole() == null || !playerData.getGuildRole().hasAtLeast(GuildRole.OFFICER)) return false;

        PlayerData targetData = plugin.getPlayerDataRepository().getPlayerDataByName(invitePlayerInput.trim());
        if (targetData == null || targetData.isInGuild()) return false;

        return plugin.getGuildManager().invitePlayer(playerData.getGuildId(), targetData.getPlayerUuid());
    }
}
