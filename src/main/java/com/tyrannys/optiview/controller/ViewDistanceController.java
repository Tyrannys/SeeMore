package com.tyrannys.optiview.controller;

import com.tyrannys.optiview.SeeMore;
import com.tyrannys.optiview.config.SeeMoreConfig;
import com.tyrannys.optiview.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class ViewDistanceController {
    private static final int MAX_UPDATE_ATTEMPTS = 10;
    private static final long CLEAN_MAP_PERIOD = 1200;
    private final SeeMore seeMore;
    private final EssentialsAfkStatusProvider afkStatusProvider;
    private final Map<UUID, Integer> clientViewDistanceMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> targetSimulationDistanceMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> targetViewDistanceMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> targetSendDistanceMap = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> undergroundRestrictedMap = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> afkRestrictedMap = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> simulationDistanceUpdateTasks = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> viewDistanceUpdateTasks = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> sendDistanceUpdateTasks = new ConcurrentHashMap<>();
    private final ViewDistanceUpdateLogger viewDistanceUpdateLogger;

    public ViewDistanceController(SeeMore seeMore) {
        this.seeMore = seeMore;
        this.afkStatusProvider = new EssentialsAfkStatusProvider(seeMore);
        this.viewDistanceUpdateLogger = new ViewDistanceUpdateLogger(seeMore);
        long undergroundCheckPeriod = Math.max(1L, seeMore.getSeeMoreConfig().underground.undergroundUpdateDelay.get());
        long afkCheckPeriod = Math.max(1L, seeMore.getSeeMoreConfig().afk.updateDelay.get());
        seeMore.getSchedulerHook().runRepeatingTask(this::cleanMaps, CLEAN_MAP_PERIOD, CLEAN_MAP_PERIOD);
        seeMore.getSchedulerHook().runRepeatingTask(this::refreshUndergroundRestrictions, undergroundCheckPeriod, undergroundCheckPeriod);
        seeMore.getSchedulerHook().runRepeatingTask(this::refreshAfkRestrictions, afkCheckPeriod, afkCheckPeriod);
        Bukkit.getPluginManager().registerEvents(new ViewDistanceUpdater(this), seeMore);
    }

    public EssentialsAfkStatusProvider getAfkStatusProvider() {
        return afkStatusProvider;
    }

    public void refreshAfkForPlayer(Player player) {
        seeMore.getSchedulerHook().runEntityTaskAsap(() -> updateAfkRestriction(player), null, player);
    }

    public void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            seeMore.getSchedulerHook().runEntityTaskAsap(() -> {
                setTargetViewDistance(player, player.getClientViewDistance(), false, true);
            }, null, player);
        }
    }

    public void setTargetViewDistance(Player player, int clientViewDistance, boolean testDelay, boolean initialUpdate) {
        clientViewDistanceMap.put(player.getUniqueId(), clientViewDistance);

        boolean undergroundRestricted = isUndergroundRestricted(player);
        undergroundRestrictedMap.put(player.getUniqueId(), undergroundRestricted);

        boolean afkRestricted = isAfkRestricted(player);
        afkRestrictedMap.put(player.getUniqueId(), afkRestricted);

        int targetSimulationDistance = getTargetSimulationDistance(player, undergroundRestricted);
        if (afkRestricted) {
            targetSimulationDistance = seeMore.getSeeMoreConfig().afk.simulationDistance.get();
        }
        targetSimulationDistanceMap.put(player.getUniqueId(), targetSimulationDistance);

        int floor = targetSimulationDistance;
        int ceiling = Math.min(seeMore.getSeeMoreConfig().worldSettings.of(player.getWorld()).maximumViewDistance.get(), 32);

        // Default to the world's view distance if the configured ceiling is negative
        ceiling = ceiling < 0 ? player.getWorld().getViewDistance() : ceiling;
        ceiling = applyUndergroundCap(ceiling, undergroundRestricted);

        int cappedClientViewDistance = clientViewDistance;
        if (afkRestricted) {
            cappedClientViewDistance = Math.min(cappedClientViewDistance, seeMore.getSeeMoreConfig().afk.renderDistance.get());
        }

        int targetViewDistance = Math.max(floor, Math.min(ceiling, cappedClientViewDistance));
        int targetSendDistance = Math.max(2, Math.min(ceiling, cappedClientViewDistance)) + 1;
        targetViewDistanceMap.put(player.getUniqueId(), targetViewDistance);
        targetSendDistanceMap.put(player.getUniqueId(), targetSendDistance);

        // Update the view distance with a delay if it is being lowered
        long delay = 0;
        try {
            if (testDelay && player.getViewDistance() > targetViewDistance) {
                delay = seeMore.getSeeMoreConfig().updateDelay.get();
            }
        } catch (Exception ignored) {}

        updateSimulationDistance(player);
        updateSendDistance(player);
        updateViewDistance(player, delay, clientViewDistance, initialUpdate);
    }

    private void updateSimulationDistance(Player player) {
        updateDistance(player, 0, 0, targetSimulationDistanceMap, simulationDistanceUpdateTasks, (p, simulationDistance) -> {
            if (p.getSimulationDistance() != simulationDistance) {
                p.setSimulationDistance(simulationDistance);
            }
        });
    }

    private void updateSendDistance(Player player) {
        updateDistance(player, 0, 0, targetSendDistanceMap, sendDistanceUpdateTasks, Player::setSendViewDistance);
    }

    private void updateViewDistance(Player player, long delay, int clientViewDistance, boolean initialUpdate) {
        updateDistance(player, delay, 0, targetViewDistanceMap, viewDistanceUpdateTasks, (p, viewDistance) -> {
            if (p.getViewDistance() != viewDistance || initialUpdate) { // always update if we've not seen them before
                p.setViewDistance(viewDistance);
                if (seeMore.getSeeMoreConfig().logChanges.get()) {
                    viewDistanceUpdateLogger.logUpdate(player, String.format("Set view distance of %s to %s (client view distance is %s).", p.getName(), viewDistance, clientViewDistance));
                }
            }
        });
    }

    private int applyUndergroundCap(int ceiling, boolean undergroundRestricted) {
        if (!undergroundRestricted) {
            return ceiling;
        }

        int undergroundMaximumViewDistance = seeMore.getSeeMoreConfig().underground.maximumViewDistance.get();
        if (undergroundMaximumViewDistance < 0) {
            return ceiling;
        }

        return Math.min(ceiling, undergroundMaximumViewDistance);
    }

    private int getTargetSimulationDistance(Player player, boolean undergroundRestricted) {
        int defaultSimulationDistance = player.getWorld().getSimulationDistance();
        if (!undergroundRestricted) {
            return defaultSimulationDistance;
        }

        int undergroundSimulationDistance = seeMore.getSeeMoreConfig().underground.simulationDistance.get();
        if (undergroundSimulationDistance < 0) {
            return defaultSimulationDistance;
        }

        return undergroundSimulationDistance;
    }

    private boolean isAfkRestricted(Player player) {
        if (!seeMore.getSeeMoreConfig().afk.enabled.get()) {
            return false;
        }
        return afkStatusProvider.isAfk(player);
    }

    private boolean isUndergroundRestricted(Player player) {
        SeeMoreConfig.UndergroundSettings underground = seeMore.getSeeMoreConfig().underground;
        if (!underground.enabled.get()) {
            return false;
        }
        if (player.getWorld().getEnvironment() != World.Environment.NORMAL) {
            return false;
        }

        double y = player.getY();
        if (y < underground.undergroundY.get()) {
            return true;
        }
        if (y > underground.surfaceY.get()) {
            return false;
        }

        return undergroundRestrictedMap.getOrDefault(player.getUniqueId(), false);
    }

    private void refreshUndergroundRestrictions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            seeMore.getSchedulerHook().runEntityTaskAsap(() -> updateUndergroundRestriction(player), null, player);
        }
    }

    private void refreshAfkRestrictions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            seeMore.getSchedulerHook().runEntityTaskAsap(() -> updateAfkRestriction(player), null, player);
        }
    }

    private void updateUndergroundRestriction(Player player) {
        UUID playerId = player.getUniqueId();
        Boolean previousRestricted = undergroundRestrictedMap.get(playerId);
        boolean undergroundRestricted = isUndergroundRestricted(player);
        if (previousRestricted != null && previousRestricted == undergroundRestricted) {
            return;
        }

        int previousViewDistance = player.getViewDistance();
        int previousSimulationDistance = player.getSimulationDistance();
        undergroundRestrictedMap.put(playerId, undergroundRestricted);
        setTargetViewDistance(player, clientViewDistanceMap.getOrDefault(playerId, player.getClientViewDistance()), false, previousRestricted == null);

        if (previousRestricted != null && seeMore.getSeeMoreConfig().logChanges.get()) {
            Integer targetViewDistance = targetViewDistanceMap.get(playerId);
            Integer targetSimulationDistance = targetSimulationDistanceMap.get(playerId);
            if (targetViewDistance != null && targetSimulationDistance != null) {
                logUndergroundTransition(player, undergroundRestricted, previousViewDistance, targetViewDistance, previousSimulationDistance, targetSimulationDistance);
            }
        }
    }

    private void updateAfkRestriction(Player player) {
        UUID playerId = player.getUniqueId();
        Boolean previousRestricted = afkRestrictedMap.get(playerId);
        boolean afkRestricted = isAfkRestricted(player);
        if (previousRestricted != null && previousRestricted == afkRestricted) {
            return;
        }

        int previousViewDistance = player.getViewDistance();
        int previousSimulationDistance = player.getSimulationDistance();
        afkRestrictedMap.put(playerId, afkRestricted);
        setTargetViewDistance(player, clientViewDistanceMap.getOrDefault(playerId, player.getClientViewDistance()), false, previousRestricted == null);

        if (previousRestricted != null && seeMore.getSeeMoreConfig().logChanges.get()) {
            Integer targetViewDistance = targetViewDistanceMap.get(playerId);
            Integer targetSimulationDistance = targetSimulationDistanceMap.get(playerId);
            if (targetViewDistance != null && targetSimulationDistance != null) {
                logAfkTransition(player, afkRestricted, previousViewDistance, targetViewDistance, previousSimulationDistance, targetSimulationDistance);
            }
        }
    }

    private void logAfkTransition(Player player, boolean afkRestricted, int previousViewDistance, int targetViewDistance, int previousSimulationDistance, int targetSimulationDistance) {
        StringJoiner changeJoiner = new StringJoiner(", ");
        if (previousViewDistance != targetViewDistance) {
            changeJoiner.add(String.format("view distance %s -> %s", previousViewDistance, targetViewDistance));
        }
        if (previousSimulationDistance != targetSimulationDistance) {
            changeJoiner.add(String.format("simulation distance %s -> %s", previousSimulationDistance, targetSimulationDistance));
        }
        if (changeJoiner.length() == 0) {
            return;
        }

        String stateMessage = afkRestricted ? "Applied AFK limits" : "Removed AFK limits";
        viewDistanceUpdateLogger.logUpdate(player, String.format("%s for %s: %s.", stateMessage, player.getName(), changeJoiner));
    }

    private void logUndergroundTransition(Player player, boolean undergroundRestricted, int previousViewDistance, int targetViewDistance, int previousSimulationDistance, int targetSimulationDistance) {
        StringJoiner changeJoiner = new StringJoiner(", ");
        if (previousViewDistance != targetViewDistance) {
            changeJoiner.add(String.format("view distance %s -> %s", previousViewDistance, targetViewDistance));
        }
        if (previousSimulationDistance != targetSimulationDistance) {
            changeJoiner.add(String.format("simulation distance %s -> %s", previousSimulationDistance, targetSimulationDistance));
        }
        if (changeJoiner.length() == 0) {
            return;
        }

        String stateMessage = undergroundRestricted ? "Applied underground limits" : "Removed underground limits";
        viewDistanceUpdateLogger.logUpdate(player, String.format("%s for %s: %s.", stateMessage, player.getName(), changeJoiner));
    }

    private void updateDistance(Player player, long delay, int attempts, Map<UUID, Integer> distanceMap, Map<UUID, ScheduledTask> taskMap, BiConsumer<Player, Integer> distanceConsumer) {
        if (attempts >= MAX_UPDATE_ATTEMPTS) {
            return; // give up if attempted too many times
        }
        Integer distance = distanceMap.get(player.getUniqueId());
        if (distance == null) {
            return; // might be null if the player has left
        }
        taskMap.compute(player.getUniqueId(), (uuid, task) -> {
            if (task != null) {
                task.cancel(); // cancel the previous task in case it is still running
            }
            if (delay > 0) {
                return seeMore.getSchedulerHook().runTaskDelayed(() -> updateDistance(player, 0, attempts, distanceMap, taskMap, distanceConsumer), delay);
            }
            CompletableFuture<ScheduledTask> retryTask = new CompletableFuture<>();
            ScheduledTask updateTask = seeMore.getSchedulerHook().runEntityTaskAsap(() -> {
                try {
                    distanceConsumer.accept(player, distance);
                } catch (Throwable ex) {

                    // will sometimes fail if the player is not attached to a world yet, so retry after 20 ticks
                    retryTask.complete(seeMore.getSchedulerHook().runTask(() -> updateDistance(player, 20, attempts + 1, distanceMap, taskMap, distanceConsumer)));
                }
            }, null, player);
            return retryTask.getNow(updateTask);
        });
    }

    private void cleanMaps() {
        clientViewDistanceMap.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        afkRestrictedMap.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        undergroundRestrictedMap.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        simulationDistanceUpdateTasks.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        sendDistanceUpdateTasks.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        targetSimulationDistanceMap.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        viewDistanceUpdateTasks.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        targetSendDistanceMap.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
        targetViewDistanceMap.entrySet().removeIf(entry -> Bukkit.getPlayer(entry.getKey()) == null);
    }

}

