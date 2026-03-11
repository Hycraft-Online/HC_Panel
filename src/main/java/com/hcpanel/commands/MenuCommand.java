package com.hcpanel.commands;

import com.hcpanel.HC_PanelPlugin;
import com.hcpanel.gui.UnifiedPanelGui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

/**
 * /menu command - Opens the unified HC_Panel character menu.
 */
public class MenuCommand extends AbstractAsyncCommand {

    private final HC_PanelPlugin plugin;

    public MenuCommand(HC_PanelPlugin plugin) {
        super("menu", "Opens the unified character panel");
        this.addAliases("panel", "charactermenu", "hcpanel");
        this.plugin = plugin;
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext ctx) {
        CommandSender sender = ctx.sender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Message.raw("This command can only be used by players.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        if (plugin.getAvailableModules().isEmpty()) {
            sender.sendMessage(Message.raw("No modules available. Install HC_Factions, HC_Honor, or HC_Attributes.")
                .color(Color.YELLOW));
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) {
            sender.sendMessage(Message.raw("Unable to open menu. Please try again.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            Ref<EntityStore> freshRef = player.getReference();
            if (freshRef == null || !freshRef.isValid()) return;

            Store<EntityStore> freshStore = freshRef.getStore();
            PlayerRef playerRef = freshStore.getComponent(freshRef, PlayerRef.getComponentType());
            if (playerRef == null) return;

            Player freshPlayer = freshStore.getComponent(freshRef, Player.getComponentType());
            if (freshPlayer == null) return;
            freshPlayer.getPageManager().openCustomPage(freshRef, freshStore,
                new UnifiedPanelGui(plugin, playerRef));
        });

        return CompletableFuture.completedFuture(null);
    }
}
