package com.tyrannys.optiview.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public class RegionisedSchedulerHook implements SchedulerHook {
    private final Plugin seeMore;

    public RegionisedSchedulerHook(Plugin seeMore) {
        this.seeMore = seeMore;
    }

    @Override
    public ScheduledTask runTask(Runnable runnable) {
        return new RegionisedScheduledTask(Bukkit.getGlobalRegionScheduler().run(seeMore, task -> runnable.run()));
    }

    @Override
    public ScheduledTask runTaskDelayed(Runnable runnable, long delay) {
        return new RegionisedScheduledTask(Bukkit.getGlobalRegionScheduler().runDelayed(seeMore, task -> runnable.run(), delay));
    }

    @Override
    public ScheduledTask runRepeatingTask(Runnable runnable, long initDelay, long period) {
        return new RegionisedScheduledTask(Bukkit.getGlobalRegionScheduler().runAtFixedRate(seeMore, task -> runnable.run(), initDelay, period));
    }

    @Override
    public ScheduledTask runEntityTask(Runnable runnable, Runnable retired, Entity entity) {
        io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduledTask = entity.getScheduler().run(seeMore, task -> runnable.run(), retired);
        return scheduledTask == null ? null : new RegionisedScheduledTask(scheduledTask);
    }

    @Override
    public ScheduledTask runEntityTaskAsap(Runnable runnable, Runnable retired, Entity entity) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            runnable.run();
            return new ScheduledTask() {
                @Override
                public void cancel() {}

                @Override
                public boolean isCancelled() {
                    return false;
                }
            };
        }
        return runEntityTask(runnable, retired, entity);
    }

    public static boolean isCompatible() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
        } catch (ClassNotFoundException e) {
            return false;
        }

        // Paper can contain some threaded-region classes; only treat the runtime as Folia
        // when the reported server distribution clearly indicates Folia.
        String version = Bukkit.getVersion();
        String name = Bukkit.getName();
        return (version != null && version.toLowerCase().contains("folia"))
                || (name != null && name.toLowerCase().contains("folia"));
    }

    private static class RegionisedScheduledTask implements ScheduledTask {
        private final io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduledTask;

        private RegionisedScheduledTask(io.papermc.paper.threadedregions.scheduler.ScheduledTask scheduledTask) {
            this.scheduledTask = scheduledTask;
        }

        @Override
        public void cancel() {
            scheduledTask.cancel();
        }

        @Override
        public boolean isCancelled() {
            return scheduledTask.isCancelled();
        }
    }

}
