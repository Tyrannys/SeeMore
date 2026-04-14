package com.tyrannys.optiview.config;

import com.froobworld.nabconfiguration.*;
import com.froobworld.nabconfiguration.annotations.Entry;
import com.froobworld.nabconfiguration.annotations.Section;
import com.froobworld.nabconfiguration.annotations.SectionMap;
import com.tyrannys.optiview.SeeMore;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class SeeMoreConfig extends NabConfiguration {
    private static final int VERSION = 4;
    private final SeeMore seeMore;

    public SeeMoreConfig(SeeMore seeMore) {
        super(
                new File(seeMore.getDataFolder(), "config.yml"),
                () -> seeMore.getResource("config.yml"),
                i -> seeMore.getResource("config-patches/" + i + ".patch"),
                VERSION
        );
        this.seeMore = seeMore;
    }

    @Override
    public void load() throws Exception {
        reconcileVersionWithContent();
        super.load();
    }

    private void reconcileVersionWithContent() {
        File configFile = new File(seeMore.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            return;
        }
        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
            int currentVersion = yaml.getInt("version", 0);
            int targetVersion = currentVersion;
            // Patch 3 adds underground.simulation-distance - skip if already present
            if (currentVersion < 3 && yaml.contains("underground.simulation-distance")) {
                targetVersion = Math.max(targetVersion, 3);
            }
            // Patch 4 adds afk section - skip if already present
            if (currentVersion < 4 && yaml.contains("afk")) {
                targetVersion = Math.max(targetVersion, 4);
            }
            if (targetVersion > currentVersion) {
                yaml.set("version", targetVersion);
                yaml.save(configFile);
                seeMore.getLogger().info("Bumped config version from " + currentVersion + " to " + targetVersion + " (fields already present, skipping patches).");
            }
        } catch (Exception ignored) {
        }
    }

    @Entry(key = "update-delay")
    public final ConfigEntry<Integer> updateDelay = new ConfigEntry<>(o -> o == null ? 600 : ((Number) o).intValue());

    @Entry(key = "log-changes")
    public final ConfigEntry<Boolean> logChanges = new ConfigEntry<>(o -> o != null && (Boolean) o);

    @Section(key = "underground")
    public final UndergroundSettings underground = new UndergroundSettings();

    @Section(key = "afk")
    public final AfkSettings afk = new AfkSettings();

    @SectionMap(key = "world-settings", defaultKey = "default")
    public final ConfigSectionMap<World, WorldSettings> worldSettings = new ConfigSectionMap<>(World::getName, WorldSettings.class, true);

    public static class UndergroundSettings extends ConfigSection {

        @Entry(key = "enabled")
        public final ConfigEntry<Boolean> enabled = new ConfigEntry<>(o -> o == null || (Boolean) o);

        @Entry(key = "update-delay")
        public final ConfigEntry<Integer> undergroundUpdateDelay = new ConfigEntry<>(o -> o == null ? 600 : ((Number) o).intValue());

        @Entry(key = "underground-y")
        public final ConfigEntry<Double> undergroundY = new ConfigEntry<>(o -> o == null ? 0.0D : ((Number) o).doubleValue());

        @Entry(key = "surface-y")
        public final ConfigEntry<Double> surfaceY = new ConfigEntry<>(o -> o == null ? 20.0D : ((Number) o).doubleValue());

        @Entry(key = "maximum-view-distance")
        public final ConfigEntry<Integer> maximumViewDistance = new ConfigEntry<>(o -> o == null ? 6 : ((Number) o).intValue());

        @Entry(key = "simulation-distance")
        public final ConfigEntry<Integer> simulationDistance = new ConfigEntry<>(o -> o == null ? 4 : ((Number) o).intValue());

    }

    public static class WorldSettings extends ConfigSection {

        @Entry(key = "maximum-view-distance")
        public final ConfigEntry<Integer> maximumViewDistance = new ConfigEntry<>(o -> o == null ? -1 : ((Number) o).intValue());

    }

    public static class AfkSettings extends ConfigSection {

        @Entry(key = "enabled")
        public final ConfigEntry<Boolean> enabled = new ConfigEntry<>(o -> o != null && (Boolean) o);

        @Entry(key = "update-delay")
        public final ConfigEntry<Integer> updateDelay = new ConfigEntry<>(o -> o == null ? 100 : ((Number) o).intValue());

        @Entry(key = "render-distance")
        public final ConfigEntry<Integer> renderDistance = new ConfigEntry<>(o -> o == null ? 4 : ((Number) o).intValue());

        @Entry(key = "simulation-distance")
        public final ConfigEntry<Integer> simulationDistance = new ConfigEntry<>(o -> o == null ? 2 : ((Number) o).intValue());

        @Section(key = "fallback")
        public final FallbackSettings fallback = new FallbackSettings();

    }

    public static class FallbackSettings extends ConfigSection {

        @Entry(key = "enabled")
        public final ConfigEntry<Boolean> enabled = new ConfigEntry<>(o -> o == null || (Boolean) o);

        @Entry(key = "timeout-ticks")
        public final ConfigEntry<Integer> timeoutTicks = new ConfigEntry<>(o -> o == null ? 6000 : ((Number) o).intValue());

    }

}

