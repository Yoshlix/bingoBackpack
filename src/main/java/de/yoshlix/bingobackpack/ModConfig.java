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

    // Item Durations & Settings
    public int lockdownDurationSeconds = 120;

    public int flightDuration1Min = 60;
    public int flightDuration5Min = 300;
    public int flightDuration15Min = 900;

    public int speedBoostDuration1Min = 60;
    public int speedBoostAmplifier1Min = 1;
    public int speedBoostDuration5Min = 300;
    public int speedBoostAmplifier5Min = 1;
    public int speedBoostDuration15Min = 900;
    public int speedBoostAmplifier15Min = 2;

    public int timeoutPlayerDurationSeconds = 150;
    public int timeoutTeamDurationSeconds = 150;

    public int levitationDurationSeconds = 10;
    public int levitationAmplifier = 0;

    public int teamShieldDurationSeconds = 30;

    public int randomTeleportMinDistance = 500;
    public int randomTeleportMaxDistance = 5000;
    public int biomeTeleportSearchRadius = 10000;
    public int structureSearchRadius = 2000;

    // Teleport Settings
    public int endTeleportSpawnX = 100;
    public int endTeleportSpawnY = 49;
    public int endTeleportSpawnZ = 0;
    public int netherCeilingY = 127;
    public int netherFallbackY = 64;
    public int safePosSearchRange = 10;

    // Item Settings
    public int deleteEnemyItemsMin = 2;
    public int deleteEnemyItemsMax = 5;
    public int itemSwapMin = 3;
    public int itemSwapMax = 8;
    public int bingoRadarScanRadius = 500;

    // Backpack Settings
    public int backpackSize = 54;

    // Bingo Integration
    public int bingoCheckIntervalTicks = 20;

    // Drop System
    public long dropCooldownMs = 60000;

    // Reward System
    public int randomGiftIntervalTicks = 600;
    public double randomGiftChance = 0.05;
    public int milestoneInterval = 5;
    public double taskCompleteItemChance = 0.15;

    // Drop Chances per Rarity (0.0 - 1.0)
    public double dropChanceCommon = 0.05;
    public double dropChanceUncommon = 0.025;
    public double dropChanceRare = 0.01;
    public double dropChanceEpic = 0.003;
    public double dropChanceLegendary = 0.001;

    // Lobby Restrictions
    public boolean lobbyDisableFishingRod = true;
    public boolean lobbyDisableLevitationPotions = true;

    // Discord Integration
    public boolean discordEnabled = false;
    public String discordToken = "";
    public String discordGuildId = "";
    public String discordTeamChannelFormat = "Bingo Team %s";
    public String discordLobbyChannelName = "Bingo Lobby";

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
