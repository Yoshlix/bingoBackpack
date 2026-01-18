package de.yoshlix.bingobackpack;

import de.yoshlix.bingobackpack.item.BingoItemCommands;
import de.yoshlix.bingobackpack.item.BingoItemCreativeTab;
import de.yoshlix.bingobackpack.item.BingoItemManager;
import de.yoshlix.bingobackpack.item.BingoItemRegistry;
import de.yoshlix.bingobackpack.item.BingoRewardSystem;
import de.yoshlix.bingobackpack.item.items.Flight1Min;
import de.yoshlix.bingobackpack.item.items.Flight5Min;
import de.yoshlix.bingobackpack.item.items.Flight15Min;
import de.yoshlix.bingobackpack.item.items.MobPheromone;
import de.yoshlix.bingobackpack.item.items.TeamShield;
import de.yoshlix.bingobackpack.item.items.TimeoutPlayer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BingoBackpack implements ModInitializer {
	public static final String MOD_ID = "bingobackpack";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("BingoBackpack mod initializing...");

		// Load config
		ModConfig.load(FabricLoader.getInstance().getConfigDir());

		// Initialize Bingo Item Registry and Creative Tab
		BingoItemRegistry.init();
		BingoItemCreativeTab.register();

		// Apply config to BingoItemManager
		BingoItemManager.getInstance().setDropsEnabled(ModConfig.getInstance().bingoItemsEnabled);
		BingoItemManager.getInstance().setGlobalDropChanceMultiplier(ModConfig.getInstance().bingoItemsDropMultiplier);

		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			BackpackCommand.register(dispatcher);
			TeleportToSpawnCommand.register(dispatcher);
			BingoItemCommands.register(dispatcher);
			UpCommand.register(dispatcher);
			StarterKitCommand.register(dispatcher);
		});

		// Initialize managers when server starts
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			TeamManager.getInstance().init(server);
			BackpackManager.getInstance().init(server);
			BingoIntegration.getInstance().init(server);
			SpawnManager.getInstance().init(server);
			BingoRewardSystem.getInstance().init(server);
			LOGGER.info("BingoBackpack initialized!");
		});

		// Save data when server stops
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			BackpackManager.getInstance().saveAll();
			LOGGER.info("BingoBackpack data saved!");
		});

		// bingo integration
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			BingoIntegration.getInstance().tick(server);
			BingoRewardSystem.getInstance().tick(server);

			// Flight expiry checks
			Flight1Min.tickFlightExpiry(server);
			Flight5Min.tickFlightExpiry(server);
			Flight15Min.tickFlightExpiry(server);

			// Timeout expiry check
			TimeoutPlayer.tickTimeoutExpiry(server);

			// Team Shield expiry check
			TeamShield.tickShieldExpiry(server);

			// Mob Pheromone spawning
			MobPheromone.tickPheromoneEffects(server);
		});
	}
}
