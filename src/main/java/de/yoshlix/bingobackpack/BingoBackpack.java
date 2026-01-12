package de.yoshlix.bingobackpack;

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

		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			BackpackCommand.register(dispatcher);
			TeleportToSpawnCommand.register(dispatcher);
		});

		// Initialize managers when server starts
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			TeamManager.getInstance().init(server);
			BackpackManager.getInstance().init(server);
			BingoIntegration.getInstance().init(server);
			SpawnManager.getInstance().init(server);
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
		});
	}
}
