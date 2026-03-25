package com.tino.enchanting.config;

import com.tino.enchanting.EnchantingRework;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class ModConfig {
    private static final String CONFIG_FILE_NAME = EnchantingRework.MOD_ID + ".properties";
    private static File configFile;

    public static float grindstoneBaseDamagePercent = 2.4f;
    public static float grindstoneValueMultiplier = 0.44f;

    public static void init() {
        configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), CONFIG_FILE_NAME);
        loadConfig();
    }

    private static void loadConfig() {
        if (!configFile.exists()) {
            saveConfig();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            Properties props = new Properties();
            props.load(reader);

            grindstoneBaseDamagePercent = getFloat(props, "grindstoneBaseDamagePercent", 2.4f);
            grindstoneValueMultiplier = getFloat(props, "grindstoneValueMultiplier", 0.44f);
            
        } catch (IOException e) {
            EnchantingRework.LOGGER.error("Failed to load config file", e);
        }
    }

    private static float getFloat(Properties props, String key, float defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Float.parseFloat(val);
        } catch (NumberFormatException e) {
            EnchantingRework.LOGGER.warn("Invalid float for " + key + " in config, using default.");
            return defaultValue;
        }
    }

    private static void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            Properties props = new Properties();
            props.setProperty("grindstoneBaseDamagePercent", String.valueOf(grindstoneBaseDamagePercent));
            props.setProperty("grindstoneValueMultiplier", String.valueOf(grindstoneValueMultiplier));
            
            String comment = "Enchanting Rework Configuration\n\n" +
                             "Formula: Damage % = BaseDamage + (EnchantmentValue * Multiplier)\n" +
                             "Default targets: 35 rerolls for Level 1, 5 rerolls for Level 30.";
            props.store(writer, comment);
        } catch (IOException e) {
            EnchantingRework.LOGGER.error("Failed to save config file", e);
        }
    }
}
