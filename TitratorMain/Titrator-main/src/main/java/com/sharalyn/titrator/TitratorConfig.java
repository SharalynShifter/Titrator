package com.sharalyn.titrator;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class TitratorConfig {

    public static boolean requireRF = true;
    public static int rfPerTick = 1;
    public static int mbPerTick = 100;
    public static int rfCapacity = 5000;
    public static int rfMaxReceive = 100;

    public static Configuration config;

    public static void init(File configFile) {
        if (configFile == null) return;
        config = new Configuration(configFile);
        config.load();

        requireRF = config.getBoolean(
            "requireRF",
            "general",
            true,
            "If true, titrator requires RF to work. If false, RF is optional bonus cost (works without power)");
        rfPerTick = config
            .getInt("rfPerTick", "general", 1, 0, 1000, "RF consumed per tick when working (1 RF/t = very cheap)");
        mbPerTick = config.getInt(
            "mbPerTick",
            "general",
            100,
            1,
            1000,
            "Fluid transferred per tick - lower = slower, more realistic. 100 = 10 ticks for bucket");
        rfCapacity = config.getInt("rfCapacity", "general", 5000, 100, 1000000, "Internal RF buffer size");
        rfMaxReceive = config.getInt("rfMaxReceive", "general", 100, 1, 10000, "Max RF that can be inserted per tick");

        if (config.hasChanged()) config.save();
    }

    public static void syncFromFile() {
        if (config == null) return;
        // DO NOT call config.load() here - GuiConfig already modified the config object in memory
        // Calling load() would discard those edits and reload old values from disk
        requireRF = config.get(Configuration.CATEGORY_GENERAL, "requireRF", true)
            .getBoolean(true);
        rfPerTick = config.get(Configuration.CATEGORY_GENERAL, "rfPerTick", 1)
            .getInt(1);
        mbPerTick = config.get(Configuration.CATEGORY_GENERAL, "mbPerTick", 100)
            .getInt(100);
        rfCapacity = config.get(Configuration.CATEGORY_GENERAL, "rfCapacity", 5000)
            .getInt(5000);
        rfMaxReceive = config.get(Configuration.CATEGORY_GENERAL, "rfMaxReceive", 100)
            .getInt(100);

        if (config.hasChanged()) config.save();
    }

    public static void syncFromDisk() {
        if (config == null) return;
        config.load();
        syncFromFile();
    }
}
