package com.tyrannys.optiview.command;

import com.tyrannys.optiview.SeeMore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.Component.*;

public class SeeMoreCommand implements CommandExecutor, TabCompleter {
    private static final Component NO_PERMISSION = text("You don't have permission to use this command.", NamedTextColor.RED);
    private final SeeMore seeMore;
    private final AverageCommand averageCommand;
    private final ReloadCommand reloadCommand;
    private final PlayersCommand playersCommand;
    private final AfkCommand afkCommand;

    public SeeMoreCommand(SeeMore seeMore) {
        this.seeMore = seeMore;
        this.averageCommand = new AverageCommand(seeMore);
        this.reloadCommand = new ReloadCommand(seeMore);
        this.playersCommand = new PlayersCommand(seeMore);
        this.afkCommand = new AfkCommand(seeMore);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("optiview.command.optiview")) {
            sender.sendMessage(NO_PERMISSION);
            return false;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("average")) {
                if (sender.hasPermission("optiview.command.average")) {
                    return averageCommand.onCommand(sender, command, label, args);
                } else {
                    sender.sendMessage(NO_PERMISSION);
                    return false;
                }
            }
            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("optiview.command.reload")) {
                    return reloadCommand.onCommand(sender, command, label, args);
                } else {
                    sender.sendMessage(NO_PERMISSION);
                    return false;
                }
            }
            if (args[0].equalsIgnoreCase("players")) {
                if (sender.hasPermission("optiview.command.players")) {
                    return playersCommand.onCommand(sender, command, label, args);
                } else {
                    sender.sendMessage(NO_PERMISSION);
                    return false;
                }
            }
            if (args[0].equalsIgnoreCase("afk")) {
                if (sender.hasPermission("optiview.command.afk")) {
                    return afkCommand.onCommand(sender, command, label, args);
                } else {
                    sender.sendMessage(NO_PERMISSION);
                    return false;
                }
            }
        }
        sender.sendMessage(text("OptiView v" + seeMore.getDescription().getVersion(), NamedTextColor.GRAY));
        sender.sendMessage(empty());
        if (sender.hasPermission("optiview.command.reload")) {
            sender.sendMessage(text("/optiview reload"));
        }
        if (sender.hasPermission("optiview.command.average")) {
            sender.sendMessage(text("/optiview average"));
        }
        if (sender.hasPermission("optiview.command.players")) {
            sender.sendMessage(text("/optiview players"));
        }
        if (sender.hasPermission("optiview.command.afk")) {
            sender.sendMessage(text("/optiview afk"));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("optiview.command.reload")) {
                suggestions.add("reload");
            }
            if (sender.hasPermission("optiview.command.average")) {
                suggestions.add("average");
            }
            if (sender.hasPermission("optiview.command.players")) {
                suggestions.add("players");
            }
            if (sender.hasPermission("optiview.command.afk")) {
                suggestions.add("afk");
            }
        }

        return StringUtil.copyPartialMatches(args[args.length - 1], suggestions, new ArrayList<>());
    }
}

