package com.froobworld.seemore.config;

import com.froobworld.nabconfiguration.*;
import com.froobworld.nabconfiguration.annotations.Entry;
import com.froobworld.nabconfiguration.annotations.Section;
import com.froobworld.nabconfiguration.annotations.SectionMap;
import com.froobworld.seemore.SeeMore;
import org.bukkit.World;

import java.io.File;

public class SeeMoreConfig extends NabConfiguration {
    private static final int VERSION = 3;

    public SeeMoreConfig(SeeMore seeMore) {
        super(
                new File(seeMore.getDataFolder(), "config.yml"),
                () -> seeMore.getResource("config.yml"),
                i -> seeMore.getResource("config-patches/" + i + ".patch"),
                VERSION
        );
    }

    @Entry(key = "update-delay")
    public final ConfigEntry<Integer> updateDelay = new ConfigEntry<>(o -> o == null ? 600 : ((Number) o).intValue());

    @Entry(key = "log-changes")
    public final ConfigEntry<Boolean> logChanges = new ConfigEntry<>(o -> o != null && (Boolean) o);

    @Section(key = "underground")
    public final UndergroundSettings underground = new UndergroundSettings();

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

}
