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

    // Drop chance per *item* of that rarity, per kill (0.0 - 1.0).
    // Pool sizes are 5/9/7/16/8, so these add up to ~26% for any drop.
    public double dropChanceCommon = 0.0193;
    public double dropChanceUncommon = 0.0096;
    public double dropChanceRare = 0.0048;
    public double dropChanceEpic = 0.0024;
    public double dropChanceLegendary = 0.0006;

    // Lobby Restrictions
    public boolean lobbyDisableFishingRod = true;
    public boolean lobbyDisableLevitationPotions = true;

    // PC prank items (fake shutdown screen, open browser, flip monitor).
    // These reach out of the game to the target player's actual machine, so they
    // are opt-in and off by default. Only affects players who have the mod
    // installed client-side; the in-game "3 hearts" debuff is not gated by this.
    public boolean pcPranksEnabled = false;
    public int monitorFlipDurationSeconds = 120;

    // Weak Heart item: cap the target's hearts for a while (in-game, always on)
    public int weakHeartsCount = 3;
    public int weakHeartsDurationSeconds = 120;

    // Gambling items
    public int crisisBetBoostDurationSeconds = 90;
    public int crisisBetPunishDurationSeconds = 45;
    public int slotMachineBuffDurationSeconds = 45;
    public int curseOrBlessingBlessingDurationSeconds = 90;
    public int curseOrBlessingCurseDurationSeconds = 60;
    public int russianRouletteHitDurationSeconds = 30;
    public int russianRouletteMissDurationSeconds = 45;
    public int diceOfFateGoodDurationSeconds = 60;
    public int fieldLockDurationSeconds = 120;

    // Chaos-Stunde: once per match, at a random point, drop chances double for
    // a while. Purely automatic — no item triggers it.
    public boolean chaosHourEnabled = true;
    public int chaosHourEarliestMinute = 10;
    public int chaosHourLatestMinute = 30;
    public int chaosHourDurationMinutes = 5;
    public double chaosHourDropMultiplier = 2.0;

    // Banish: periodic server-wide status broadcast while a player is stuck
    // in a banish task, so the rest of the server can see who's held up.
    public boolean banishStatusBroadcastEnabled = true;
    public int banishStatusBroadcastIntervalSeconds = 30;

    // Bounty-Board: rotating world objectives, earned (not RNG-scaled) rewards.
    public boolean bountyBoardEnabled = true;
    public int bountyRotationMinutes = 10;

    // Momentum-Leiste: team meter charged by kills/objectives/rows/banish
    // escapes, unlocks one earned team ability (bonus or harmful) at 100%.
    public double momentumChargePerKill = 0.5;
    public double momentumChargePerObjective = 8.0;
    public double momentumChargePerRow = 25.0;
    public double momentumChargePerBanishEscape = 15.0;
    public int momentumAnsturmDurationSeconds = 180;
    public int momentumBollwerkDurationSeconds = 300;
    public int momentumSpuersinnDurationSeconds = 300;
    public int momentumVerdunkelungDurationSeconds = 60;
    public int momentumStillstandDurationSeconds = 20;

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
