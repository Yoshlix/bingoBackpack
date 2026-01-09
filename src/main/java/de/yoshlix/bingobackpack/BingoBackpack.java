package de.yoshlix.bingobackpack;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BingoBackpack implements ModInitializer {
	public static final String MOD_ID = "bingobackpack";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("BingoBackpack mod initializing...");

		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			BackpackCommand.register(dispatcher);
			TrollCommand.register(dispatcher);
		});

		// Initialize managers when server starts
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			TeamManager.getInstance().init(server);
			BackpackManager.getInstance().init(server);
			TrollManager.getInstance().init(server);
			BingoIntegration.getInstance().init(server);
			LOGGER.info("BingoBackpack initialized!");
		});

		// Save data when server stops
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			BackpackManager.getInstance().saveAll();
			LOGGER.info("BingoBackpack data saved!");
		});

		// Tick trolls and bingo integration
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			TrollManager.getInstance().tick(server);
			BingoIntegration.getInstance().tick(server);
		});
	}
}
