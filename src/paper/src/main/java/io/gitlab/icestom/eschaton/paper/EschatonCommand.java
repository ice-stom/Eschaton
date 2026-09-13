package io.gitlab.icestom.eschaton.paper;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class EschatonCommand {

    private static final String PERMISSION = "eschaton.admin";

    private EschatonCommand() {
    }

    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS = (ctx, builder) -> {
        String remaining = builder.getRemaining().toLowerCase();
        for (Player online : Bukkit.getOnlinePlayers()) {
            String name = online.getName();
            if (name.toLowerCase().startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return CompletableFuture.completedFuture(builder.build());
    };

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("eschaton")
                .executes(ctx -> {
                    sendUsage(ctx.getSource().getSender());
                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("set")
                        .requires(source -> source.getSender().hasPermission(PERMISSION))
                        .then(Commands.literal("on")
                                .executes(ctx -> {
                                    EschatonPlugin.instance.setEschatonActive(true);
                                    ctx.getSource().getSender().sendMessage(
                                            Component.text("[Eschaton] Enabled.", NamedTextColor.GREEN));
                                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("off")
                                .executes(ctx -> {
                                    EschatonPlugin.instance.setEschatonActive(false);
                                    ctx.getSource().getSender().sendMessage(
                                            Component.text("[Eschaton] Disabled.", NamedTextColor.GOLD));
                                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("ignore")
                        .requires(source -> source.getSender().hasPermission(PERMISSION))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(ONLINE_PLAYERS)
                                .executes(ctx -> handleIgnore(EschatonPlugin.instance, ctx, true))))
                .then(Commands.literal("unignore")
                        .requires(source -> source.getSender().hasPermission(PERMISSION))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(ONLINE_PLAYERS)
                                .executes(ctx -> handleIgnore(EschatonPlugin.instance, ctx, false))))
                .then(Commands.literal("subscribe")
                        .requires(source -> source.getSender().hasPermission(PERMISSION))
                        .executes(ctx -> handleSubscribe(EschatonPlugin.instance, ctx, true)))
                .then(Commands.literal("unsubscribe")
                        .requires(source -> source.getSender().hasPermission(PERMISSION))
                        .executes(ctx -> handleSubscribe(EschatonPlugin.instance, ctx, false)));
    }

    private static int handleIgnore(EschatonPlugin plugin, CommandContext<CommandSourceStack> ctx, boolean ignore) {
        CommandSender sender = ctx.getSource().getSender();
        String name = StringArgumentType.getString(ctx, "player");

        OfflinePlayer target = Bukkit.getOfflinePlayer(name);
        UUID uuid = target.getUniqueId();
        String displayName = target.getName() != null ? target.getName() : name;

        if (ignore) {
            plugin.ignore(uuid);
            sender.sendMessage(Component.text("[Eschaton] Now ignoring " + displayName + ".", NamedTextColor.GREEN));
        } else {
            plugin.unignore(uuid);
            sender.sendMessage(Component.text("[Eschaton] No longer ignoring " + displayName + ".", NamedTextColor.GREEN));
        }

        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private static int handleSubscribe(EschatonPlugin plugin, CommandContext<CommandSourceStack> ctx, boolean subscribe) {
        CommandSender sender = ctx.getSource().getSender();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can subscribe to alerts.", NamedTextColor.RED));
            return 0;
        }

        if (subscribe) {
            plugin.subscribe(player.getUniqueId());
            sender.sendMessage(Component.text("[Eschaton] Subscribed to alerts.", NamedTextColor.GREEN));
        } else {
            plugin.unsubscribe(player.getUniqueId());
            sender.sendMessage(Component.text("[Eschaton] Unsubscribed from alerts.", NamedTextColor.GOLD));
        }

        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private static void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/eschaton set <on|off>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/eschaton ignore <player>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/eschaton unignore <player>", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/eschaton subscribe", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/eschaton unsubscribe", NamedTextColor.YELLOW));
    }
}