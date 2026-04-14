package com.tyrannys.optiview;

import com.tyrannys.optiview.command.SeeMoreCommand;
import com.tyrannys.optiview.config.SeeMoreConfig;
import com.tyrannys.optiview.controller.ViewDistanceController;
import com.tyrannys.optiview.metrics.SeeMoreMetrics;
import com.tyrannys.optiview.scheduler.BukkitSchedulerHook;
import com.tyrannys.optiview.scheduler.RegionisedSchedulerHook;
import com.tyrannys.optiview.scheduler.SchedulerHook;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class SeeMore extends JavaPlugin {
    private SeeMoreConfig config;
    private SchedulerHook schedulerHook;
    private ViewDistanceController viewDistanceController;

    @Override
    public void onEnable() {
        config = new SeeMoreConfig(this);
        try {
            config.load();
        } catch (Exception e) {
            getLogger().severe("Error loading config");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (RegionisedSchedulerHook.isCompatible()) {
            schedulerHook = new RegionisedSchedulerHook(this);
        } else {
            schedulerHook = new BukkitSchedulerHook(this);
        }

        viewDistanceController = new ViewDistanceController(this);

        registerCommand();

        new SeeMoreMetrics(this);
    }

    @Override
    public void onDisable() {

    }

    private void registerCommand() {
        PluginCommand pluginCommand = getCommand("optiview");
        if (pluginCommand != null) {
            SeeMoreCommand seeMoreCommand = new SeeMoreCommand(this);
            pluginCommand.setExecutor(seeMoreCommand);
            pluginCommand.setTabCompleter(seeMoreCommand);
            pluginCommand.setPermission("optiview.command.optiview");
        }
    }

    public void reload() throws Exception {
        config.load();
        if (viewDistanceController != null) {

            // Update the target view distance of all players in case the configured maximum has changed
            viewDistanceController.updateAllPlayers();
        }
    }

    public SeeMoreConfig getSeeMoreConfig() {
        return config;
    }

    public SchedulerHook getSchedulerHook() {
        return schedulerHook;
    }
}

