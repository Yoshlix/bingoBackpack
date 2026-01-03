package de.yoshlix.bingobackpack;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class BackpackManager {
    private static final String BACKPACKS_DIR = "bingobackpack";
    private static final int BACKPACK_SIZE = 54; // Double chest size

    // Map of team name to active container (for syncing between players)
    private final Map<String, SimpleContainer> activeContainers = new HashMap<>();

    private Path dataDir;
    private MinecraftServer server;

    public void init(MinecraftServer server) {
        this.server = server;
        this.dataDir = server.getWorldPath(LevelResource.ROOT).resolve(BACKPACKS_DIR);
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            BingoBackpack.LOGGER.error("Failed to create backpack data directory", e);
        }
    }

    public void openBackpack(ServerPlayer player, String teamName) {
        SimpleContainer container = getOrCreateContainer(teamName);
        
        player.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, playerEntity) -> 
                    new ChestMenu(MenuType.GENERIC_9x6, syncId, playerInventory, container, 6),
                Component.literal("Team Backpack: " + teamName)
        ));
    }

    private SimpleContainer getOrCreateContainer(String teamName) {
        // Check if we have an active container
        SimpleContainer container = activeContainers.get(teamName);
        if (container != null) {
            return container;
        }

        // Create new container and load saved data
        container = new BackpackContainer(BACKPACK_SIZE, teamName);
        loadContainer(container, teamName);

        activeContainers.put(teamName, container);
        return container;
    }

    public void saveBackpack(String teamName) {
        SimpleContainer container = activeContainers.get(teamName);
        if (container != null) {
            saveContainer(container, teamName);
        }
    }

    public void clearBackpack(String teamName) {
        activeContainers.remove(teamName);
        Path backpackFile = dataDir.resolve(teamName + ".dat");
        try {
            Files.deleteIfExists(backpackFile);
        } catch (IOException e) {
            BingoBackpack.LOGGER.error("Failed to delete backpack file for team: " + teamName, e);
        }
    }

    private void saveContainer(Container container, String teamName) {
        if (server == null || dataDir == null) return;
        
        try {
            CompoundTag rootTag = new CompoundTag();
            ListTag listTag = new ListTag();
            HolderLookup.Provider registries = server.registryAccess();
            
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty()) {
                    CompoundTag itemTag = new CompoundTag();
                    itemTag.putInt("Slot", i);
                    itemTag.put("Item", stack.save(registries));
                    listTag.add(itemTag);
                }
            }
            
            rootTag.put("Items", listTag);
            
            Path backpackFile = dataDir.resolve(teamName + ".dat");
            NbtIo.writeCompressed(rootTag, backpackFile);
        } catch (IOException e) {
            BingoBackpack.LOGGER.error("Failed to save backpack for team: " + teamName, e);
        }
    }

    private void loadContainer(Container container, String teamName) {
        if (server == null || dataDir == null) return;
        
        Path backpackFile = dataDir.resolve(teamName + ".dat");
        if (!Files.exists(backpackFile)) return;
        
        try {
            CompoundTag rootTag = NbtIo.readCompressed(backpackFile, NbtAccounter.unlimitedHeap());
            ListTag listTag = rootTag.getList("Items", 10); // 10 = CompoundTag
            HolderLookup.Provider registries = server.registryAccess();
            
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag itemTag = listTag.getCompound(i);
                int slot = itemTag.getInt("Slot");
                if (slot >= 0 && slot < container.getContainerSize()) {
                    ItemStack stack = ItemStack.parse(registries, itemTag.getCompound("Item")).orElse(ItemStack.EMPTY);
                    container.setItem(slot, stack);
                }
            }
        } catch (IOException e) {
            BingoBackpack.LOGGER.error("Failed to load backpack for team: " + teamName, e);
        }
    }

    public void saveAll() {
        for (Map.Entry<String, SimpleContainer> entry : activeContainers.entrySet()) {
            saveContainer(entry.getValue(), entry.getKey());
        }
    }

    // Inner class for backpack container that auto-saves on changes
    private class BackpackContainer extends SimpleContainer {
        private final String teamName;

        public BackpackContainer(int size, String teamName) {
            super(size);
            this.teamName = teamName;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            saveBackpack(teamName);
        }
    }

    // Singleton instance
    private static BackpackManager instance;

    public static BackpackManager getInstance() {
        if (instance == null) {
            instance = new BackpackManager();
        }
        return instance;
    }

    private BackpackManager() {}
}
