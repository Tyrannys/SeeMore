package com.tyrannys.optiview.controller;

import com.tyrannys.optiview.SeeMore;

import io.papermc.paper.event.player.AsyncChatEvent;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class EssentialsAfkStatusProvider {
    private final Object essentialsPlugin;
    private final Method getUserMethod;
    private final Method isAfkMethod;
    private final Method setAfkMethod;
    private final String pluginName;
    private final SeeMore seeMore;
    private final Map<UUID, Long> lastActivityMsByPlayer = new ConcurrentHashMap<>();
    private final Set<UUID> manuallyAfkPlayers = ConcurrentHashMap.newKeySet();

    public EssentialsAfkStatusProvider(SeeMore seeMore) {
        this.seeMore = seeMore;
        Object foundPlugin = null;
        Method foundGetUserMethod = null;
        Method foundIsAfkMethod = null;

        Plugin plugin = Bukkit.getPluginManager().getPlugin("EssentialsX");
        if (plugin == null) {
            plugin = Bukkit.getPluginManager().getPlugin("Essentials");
        }

        String foundPluginName = null;
        if (plugin != null) {
            try {
                Method getUser = plugin.getClass().getMethod("getUser", Player.class);
                Class<?> userClass = getUser.getReturnType();
                Method isAfk = userClass.getMethod("isAfk");

                foundPlugin = plugin;
                foundGetUserMethod = getUser;
                foundIsAfkMethod = isAfk;
                foundPluginName = plugin.getName();
                seeMore.getLogger().info("Hooked into " + foundPluginName + " AFK API.");
            } catch (ReflectiveOperationException ex) {
                seeMore.getLogger().log(Level.WARNING, "Found " + plugin.getName() + " but could not hook AFK API. AFK overrides will be disabled.", ex);
            }
        }

        Method foundSetAfkMethod = null;
        if (foundGetUserMethod != null) {
            try {
                foundSetAfkMethod = foundGetUserMethod.getReturnType().getMethod("setAfk", boolean.class);
            } catch (ReflectiveOperationException ignored) {}
        }

        this.essentialsPlugin = foundPlugin;
        this.getUserMethod = foundGetUserMethod;
        this.isAfkMethod = foundIsAfkMethod;
        this.setAfkMethod = foundSetAfkMethod;
        this.pluginName = foundPluginName;

        Bukkit.getPluginManager().registerEvents(new FallbackAfkActivityListener(), seeMore);
    }

    public boolean isAvailable() {
        return essentialsPlugin != null && getUserMethod != null && isAfkMethod != null;
    }

    public String getPluginName() {
        return pluginName;
    }

    public boolean isAfk(Player player) {
        if (isAvailable()) {
            try {
                Object user = getUserMethod.invoke(essentialsPlugin, player);
                if (user == null) {
                    return false;
                }
                Object afk = isAfkMethod.invoke(user);
                return afk instanceof Boolean && (Boolean) afk;
            } catch (ReflectiveOperationException ex) {
                return false;
            }
        }

        return isFallbackAfk(player);
    }

    /**
     * Toggles the AFK state of a player. Uses EssentialsX when available, otherwise
     * uses the built-in manual AFK set. Manual AFK is sticky and is not cleared by movement.
     *
     * @return true if the player is now AFK, false if they are now active.
     */
    public boolean toggleManualAfk(Player player) {
        if (isAvailable() && setAfkMethod != null) {
            try {
                Object user = getUserMethod.invoke(essentialsPlugin, player);
                if (user != null) {
                    boolean current = (Boolean) isAfkMethod.invoke(user);
                    setAfkMethod.invoke(user, !current);
                    return !current;
                }
            } catch (ReflectiveOperationException ignored) {
                // Fall through to internal fallback
            }
        }
        UUID id = player.getUniqueId();
        if (manuallyAfkPlayers.remove(id)) {
            lastActivityMsByPlayer.put(id, System.currentTimeMillis());
            return false;
        }
        manuallyAfkPlayers.add(id);
        return true;
    }

    private boolean isFallbackAfk(Player player) {
        if (!seeMore.getSeeMoreConfig().afk.fallback.enabled.get()) {
            return false;
        }
        if (manuallyAfkPlayers.contains(player.getUniqueId())) {
            return true;
        }
        long timeoutTicks = Math.max(1L, seeMore.getSeeMoreConfig().afk.fallback.timeoutTicks.get());
        long timeoutMs = timeoutTicks * 50L;
        long now = System.currentTimeMillis();
        Long lastActivity = lastActivityMsByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> now);
        return now - lastActivity >= timeoutMs;
    }

    private void markActive(Player player) {
        if (manuallyAfkPlayers.contains(player.getUniqueId())) {
            return; // Manual AFK is sticky — activity does not clear it.
        }
        lastActivityMsByPlayer.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private boolean shouldTrackFallbackActivity() {
        return !isAvailable() && seeMore.getSeeMoreConfig().afk.fallback.enabled.get();
    }

    private class FallbackAfkActivityListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        private void onMove(PlayerMoveEvent event) {
            if (!shouldTrackFallbackActivity()) {
                return;
            }

            Location from = event.getFrom();
            Location to = event.getTo();
            if (to == null) {
                return;
            }

            // Only consider block movement as activity to avoid jitter from head movement.
            if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
                return;
            }
            markActive(event.getPlayer());
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        private void onChat(AsyncChatEvent event) {
            if (!shouldTrackFallbackActivity()) {
                return;
            }
            markActive(event.getPlayer());
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        private void onCommand(PlayerCommandPreprocessEvent event) {
            if (!shouldTrackFallbackActivity()) {
                return;
            }
            markActive(event.getPlayer());
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        private void onInteract(PlayerInteractEvent event) {
            if (!shouldTrackFallbackActivity()) {
                return;
            }
            markActive(event.getPlayer());
        }

        @EventHandler(priority = EventPriority.MONITOR)
        private void onJoin(PlayerJoinEvent event) {
            lastActivityMsByPlayer.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }

        @EventHandler(priority = EventPriority.MONITOR)
        private void onQuit(PlayerQuitEvent event) {
            lastActivityMsByPlayer.remove(event.getPlayer().getUniqueId());
            manuallyAfkPlayers.remove(event.getPlayer().getUniqueId());
        }
    }
}
