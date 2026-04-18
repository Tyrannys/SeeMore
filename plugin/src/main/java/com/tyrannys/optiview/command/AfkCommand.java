package com.tyrannys.optiview.command;

import com.tyrannys.optiview.SeeMore;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static net.kyori.adventure.text.Component.text;

public class AfkCommand implements CommandExecutor, TabCompleter {
    private final SeeMore seeMore;

    public AfkCommand(SeeMore seeMore) {
        this.seeMore = seeMore;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }

        boolean nowAfk = seeMore.getViewDistanceController().getAfkStatusProvider().toggleManualAfk(player);

        if (nowAfk) {
            player.sendMessage(text("You are now AFK.", NamedTextColor.GRAY));
        } else {
            player.sendMessage(text("You are no longer AFK.", NamedTextColor.GRAY));
        }

        seeMore.getViewDistanceController().refreshAfkForPlayer(player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
