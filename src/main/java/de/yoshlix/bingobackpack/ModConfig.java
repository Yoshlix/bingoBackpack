package de.yoshlix.bingobackpack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ModConfig {
    private static ModConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean hungerMixinEnabled = false;
    public boolean spawnTeleportEnabled = false;
    public boolean bingoItemsEnabled = true;
    public double bingoItemsDropMultiplier = 1.0;

    public static ModConfig getInstance() {
        if (instance == null) {
            instance = new ModConfig();
        }
        return instance;
    }

    public static void load(Path configDir) {
        File configFile = configDir.resolve("bingobackpack.json").toFile();

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                instance = GSON.fromJson(reader, ModConfig.class);
                BingoBackpack.LOGGER.info("Config loaded successfully");
            } catch (Exception e) {
                BingoBackpack.LOGGER.error("Failed to load config, using defaults", e);
                instance = new ModConfig();
            }
        } else {
            instance = new ModConfig();
            save(configDir);
        }
    }

    public static void save(Path configDir) {
        File configFile = configDir.resolve("bingobackpack.json").toFile();
        configFile.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(getInstance(), writer);
            BingoBackpack.LOGGER.info("Config saved successfully");
        } catch (Exception e) {
            BingoBackpack.LOGGER.error("Failed to save config", e);
        }
    }
}
