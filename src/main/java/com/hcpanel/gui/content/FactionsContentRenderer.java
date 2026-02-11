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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Renders Factions module content into the unified panel.
 */
public class FactionsContentRenderer {

    private final PlayerRef playerRef;

    public FactionsContentRenderer(PlayerRef playerRef) {
        this.playerRef = playerRef;
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

    /**
     * Sets up the shared faction header (tag + player/guild name).
     */
    private void setupFactionHeader(UICommandBuilder cmd, Faction faction, String title, String subtitle) {
        cmd.set("#HeaderSection.Visible", false);
        cmd.set("#FactionHeaderSection.Visible", true);

        if (faction != null) {
            cmd.set("#FactionTagLarge.Text", "[" + faction.getShortName() + "]");
            cmd.set("#FactionTagLarge.Style.TextColor", faction.getColorHex());
        } else {
            cmd.set("#FactionTagLarge.Text", "[???]");
            cmd.set("#FactionTagLarge.Style.TextColor", "#96a9be");
        }

        cmd.set("#FactionPlayerName.Text", title);
        if (subtitle != null) {
            cmd.set("#FactionGuildName.Text", subtitle);
        }
    }

    private String getRoleColor(GuildRole role) {
        if (role == null) return "#ffffff";
        return switch (role) {
            case LEADER -> "#FFD700";
            case OFFICER -> "#4FC3F7";
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
                               PlayerRef playerRef, String view) {
        if (!isFactionsAvailable()) {
            cmd.set("#ContentText.Visible", true);
            cmd.set("#ContentText.Text", "Factions system not available.");
            return;
        }

        if (view == null) view = "guild";

        switch (view) {
            case "guild" -> renderGuild(cmd, events);
            case "members" -> renderMembers(cmd, events);
            case "browser" -> renderBrowser(cmd, events);
            case "invitations" -> renderInvitations(cmd, events);
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

        setupFactionHeader(cmd, faction, playerRef.getUsername(), guild.getName());

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
        if (claimCount >= maxClaims) {
            cmd.set("#ClaimsDesc.Text", "at capacity!");
            cmd.set("#ClaimsValue.Style.TextColor", "#ff6b6b");
        } else {
            cmd.set("#ClaimsDesc.Text", "chunks claimed");
        }

        int powerPerMember = plugin.getConfig().getGuildDefaultPower();
        cmd.set("#PowerFormula.Text", memberCount + " x " + powerPerMember + " = " + power);
        cmd.set("#PowerPerMember.Text", "+" + powerPerMember + " power per member");
        cmd.set("#MaxClaimsInfo.Text", "Max claims: " + maxClaims);

        // Tag editor (officers+)
        if (guildRole != null && guildRole.hasAtLeast(GuildRole.OFFICER)) {
            cmd.set("#GuildTagSection.Visible", true);

            String currentTag = guild.getTag();
            if (currentTag != null && !currentTag.isEmpty()) {
                cmd.set("#CurrentTagLabel.Text", "Current: [" + currentTag + "]");
            } else {
                cmd.set("#CurrentTagLabel.Text", "No tag set");
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
            "Faction: " + factionName + "\n" +
            "Power determines your max land claims.\n" +
            "Each active member contributes +" + powerPerMember + " power.");

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

        setupFactionHeader(cmd, faction, playerRef.getUsername(), null);
        cmd.set("#FactionGuildName.Visible", false);

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

    private void renderMembers(UICommandBuilder cmd, UIEventBuilder events) {
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

        setupFactionHeader(cmd, faction, guild.getName(), "Guild Members");

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

        int index = 0;
        for (PlayerData member : members) {
            if (index >= 7) break;

            UUID memberUuid = member.getPlayerUuid();
            GuildRole memberRole = member.getGuildRole();
            String roleColor = getRoleColor(memberRole);

            cmd.set("#MemberRow" + index + ".Visible", true);
            cmd.set("#MemberName" + index + ".Text", member.getPlayerName());
            cmd.set("#MemberRole" + index + ".Text",
                memberRole != null ? "[" + memberRole.getDisplayName() + "]" : "[Member]");
            cmd.set("#MemberRole" + index + ".Style.TextColor", roleColor);
            cmd.set("#OnlineIndicator" + index + ".Background", "#757575");

            boolean isSelf = memberUuid.equals(playerRef.getUuid());
            boolean canManage = myRole != null && !isSelf && canManageMember(myRole, memberRole);

            if (canManage) {
                String memberIdStr = memberUuid.toString();

                boolean canPromote = memberRole != null && memberRole != GuildRole.LEADER &&
                    (myRole == GuildRole.LEADER || memberRole.ordinal() > myRole.ordinal() + 1);
                if (canPromote) {
                    cmd.set("#MemberPromote" + index + ".Visible", true);
                    events.addEventBinding(CustomUIEventBindingType.Activating, "#MemberPromote" + index,
                        EventData.of("Action", "action:promote:" + memberIdStr), false);
                }

                boolean canDemote = memberRole != null && memberRole != GuildRole.RECRUIT &&
                    memberRole != GuildRole.LEADER;
                if (canDemote) {
                    cmd.set("#MemberDemote" + index + ".Visible", true);
                    events.addEventBinding(CustomUIEventBindingType.Activating, "#MemberDemote" + index,
                        EventData.of("Action", "action:demote:" + memberIdStr), false);
                }

                cmd.set("#MemberKick" + index + ".Visible", true);
                events.addEventBinding(CustomUIEventBindingType.Activating, "#MemberKick" + index,
                    EventData.of("Action", "action:kick:" + memberIdStr), false);
            }

            index++;
        }

        for (int i = index; i < 7; i++) {
            cmd.set("#MemberRow" + i + ".Visible", false);
        }

        cmd.set("#FooterText.Text", "^ Promote  v Demote  X Kick");
    }

    private void renderBrowser(UICommandBuilder cmd, UIEventBuilder events) {
        HC_FactionsPlugin plugin = getPlugin();
        PlayerData playerData = resolvePlayerData();
        Faction faction = resolveFaction();

        setupFactionHeader(cmd, faction, "Guild Browser", "Find a guild to join");

        String playerFactionId = playerData != null ? playerData.getFactionId() : null;
        if (playerFactionId == null) {
            cmd.set("#FactionQuickInfo.Visible", true);
            cmd.set("#QuickInfoText.Text", "You must select a faction before browsing guilds.\n\nUse /faction to choose your faction.");
            cmd.set("#FooterText.Text", "Select a faction first.");
            return;
        }

        cmd.set("#GuildListSection.Visible", true);
        boolean playerInGuild = playerData.isInGuild();
        List<Guild> guilds = plugin.getGuildManager().getGuildsByFaction(playerFactionId);

        int index = 0;
        for (Guild guildObj : guilds) {
            if (index >= 6) break;

            List<PlayerData> members = plugin.getGuildManager().getGuildMembers(guildObj.getId());
            int memberCount = members != null ? members.size() : 0;
            int maxMembers = plugin.getConfig().getGuildMaxMembers();

            cmd.set("#GuildRow" + index + ".Visible", true);
            cmd.set("#GuildName" + index + ".Text", guildObj.getName());
            cmd.set("#GuildMembers" + index + ".Text", memberCount + "/" + maxMembers);
            cmd.set("#GuildPower" + index + ".Text", guildObj.getPower() + " pwr");

            if (playerInGuild) {
                cmd.set("#GuildJoin" + index + ".Visible", false);
            } else {
                cmd.set("#GuildJoin" + index + ".Visible", true);
                events.addEventBinding(CustomUIEventBindingType.Activating, "#GuildJoin" + index,
                    EventData.of("Action", "action:guild_join:" + guildObj.getId().toString()), false);
            }

            index++;
        }

        for (int i = index; i < 6; i++) {
            cmd.set("#GuildRow" + i + ".Visible", false);
        }

        if (index == 0) {
            cmd.set("#GuildListSection.Visible", false);
            cmd.set("#FactionQuickInfo.Visible", true);
            cmd.set("#QuickInfoText.Text", "No guilds found for your faction.\n\nBe the first to create one using /guild create <name>");
        }

        cmd.set("#FooterText.Text", playerInGuild
            ? "You are already in a guild. Leave your current guild to join another."
            : "Click JOIN to request membership.");
    }

    private void renderInvitations(UICommandBuilder cmd, UIEventBuilder events) {
        HC_FactionsPlugin plugin = getPlugin();
        Faction faction = resolveFaction();

        setupFactionHeader(cmd, faction, "Invitations", "Pending guild invites");

        cmd.set("#InviteListSection.Visible", true);
        List<GuildInvitation> invitations = plugin.getGuildManager().getPendingInvitations(playerRef.getUuid());

        int index = 0;
        for (GuildInvitation invitation : invitations) {
            if (index >= 5) break;

            UUID guildId = invitation.getGuildId();
            Guild guildObj = plugin.getGuildManager().getGuild(guildId);
            String guildName = guildObj != null ? guildObj.getName() : "Unknown Guild";

            cmd.set("#InviteRow" + index + ".Visible", true);
            cmd.set("#InviteGuildName" + index + ".Text", guildName);
            cmd.set("#InviteFrom" + index + ".Text", "from " + invitation.getInviterName());

            String guildIdStr = guildId.toString();
            events.addEventBinding(CustomUIEventBindingType.Activating, "#InviteAccept" + index,
                EventData.of("Action", "action:invite_accept:" + guildIdStr), false);
            events.addEventBinding(CustomUIEventBindingType.Activating, "#InviteDecline" + index,
                EventData.of("Action", "action:invite_decline:" + guildIdStr), false);

            index++;
        }

        for (int i = index; i < 5; i++) {
            cmd.set("#InviteRow" + i + ".Visible", false);
        }

        if (index == 0) {
            cmd.set("#InviteListSection.Visible", false);
            cmd.set("#FactionQuickInfo.Visible", true);
            cmd.set("#QuickInfoText.Text", "No pending invitations.\n\nBrowse guilds and request to join, or wait for guild officers to invite you.");
        }

        cmd.set("#FooterText.Text", "Click ACCEPT or DECLINE to respond to invites.");
    }

    private void renderOverview(UICommandBuilder cmd, UIEventBuilder events) {
        Faction faction = resolveFaction();
        if (faction == null) {
            setupFactionHeader(cmd, null, "Faction Overview", "No faction selected");
            cmd.set("#FactionQuickInfo.Visible", true);
            cmd.set("#QuickInfoText.Text", "You must select a faction first.\n\nUse /faction to choose your faction.");
            cmd.set("#FooterText.Text", "Select a faction to view its overview.");
            return;
        }

        HC_FactionsPlugin plugin = getPlugin();
        setupFactionHeader(cmd, faction, faction.getDisplayName(), "Faction Overview");
        cmd.set("#FactionPlayerName.Style.TextColor", faction.getColorHex());

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
        cmd.set("#RankValue.Text", String.valueOf(guildCount));
        cmd.set("#RankDesc.Text", "active guilds");
        cmd.set("#MemberCountValue.Text", String.valueOf(totalMembers));
        cmd.set("#MembersDesc.Text", "total players");

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
