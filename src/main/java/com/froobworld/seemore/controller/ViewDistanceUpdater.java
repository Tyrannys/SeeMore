package com.froobworld.seemore.controller;

import com.destroystokyo.paper.event.player.PlayerClientOptionsChangeEvent;
import com.google.common.collect.Sets;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

import com.froobworld.seemore.SeeMore;

public class ViewDistanceUpdater implements Listener {
    private final ViewDistanceController controller;
    private final SeeMore seeMore;
    private final Set<UUID> seenBefore = Sets.newConcurrentHashSet();

    public ViewDistanceUpdater(ViewDistanceController viewDistanceController, SeeMore seeMore) {
        this.controller = viewDistanceController;
        this.seeMore = seeMore;
    }

    @EventHandler
    private void onOptionsChange(PlayerClientOptionsChangeEvent event) {

        // the change check may fail if the player has just joined the server, so also check if we have seen them before
        boolean seen = seenBefore.contains(event.getPlayer().getUniqueId());

        if (event.hasViewDistanceChanged() || !seen) {
            seenBefore.add(event.getPlayer().getUniqueId());
            controller.setTargetViewDistance(event.getPlayer(), event.getViewDistance(), seen, !seen);
        }
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        seenBefore.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    private void onWorldChange(PlayerChangedWorldEvent event) {
        controller.setTargetViewDistance(event.getPlayer(), event.getPlayer().getClientViewDistance(), false, false);
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 延迟10tick（0.5秒）后主动根据客户端视距调整服务器视距
        seeMore.getSchedulerHook().runTaskDelayed(() -> {
            controller.setTargetViewDistance(player, player.getClientViewDistance(), false, true);
        }, 10L);
    }

}
