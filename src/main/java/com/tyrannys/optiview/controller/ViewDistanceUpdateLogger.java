package com.tyrannys.optiview.controller;

import com.tyrannys.optiview.SeeMore;
import com.tyrannys.optiview.scheduler.ScheduledTask;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ViewDistanceUpdateLogger {
    private final SeeMore seeMore;
    private final Map<UUID, ScheduledTask> updateMessageTasks;

    public ViewDistanceUpdateLogger(SeeMore seeMore) {
        this.seeMore = seeMore;
        this.updateMessageTasks = new ConcurrentHashMap<>();
    }

    public void logUpdate(Player player, String logMessage) {
        updateMessageTasks.compute(player.getUniqueId(), (uuid, oldTask) -> {
            if (oldTask != null) {
                oldTask.cancel();
            }
            return seeMore.getSchedulerHook().runTaskDelayed(() -> {
                seeMore.getSLF4JLogger().info(logMessage);
                updateMessageTasks.remove(uuid);
            }, 20); // delay by 20 ticks to avoid spam from vanilla clients using the view distance slider
        });
    }

}

