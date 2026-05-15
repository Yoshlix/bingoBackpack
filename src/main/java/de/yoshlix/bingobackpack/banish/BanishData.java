package de.yoshlix.bingobackpack.banish;

import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BanishData {
    public UUID playerId;
    public String originalDimension;
    public double originalX, originalY, originalZ;
    public float originalYaw, originalPitch;
    public int taskIndex;
    public double taskSpawnX, taskSpawnY, taskSpawnZ;
    public int taskTime = 0;
    public int originX, originY, originZ;
    public List<SerializedItem> savedInventory = new ArrayList<>();

    public static BanishData from(ServerPlayer p, int idx) {
        BanishData s = new BanishData();
        s.playerId = p.getUUID();
        s.originalDimension = p.level().dimension().identifier().toString();
        s.originalX = p.getX();
        s.originalY = p.getY();
        s.originalZ = p.getZ();
        s.originalYaw = p.getYRot();
        s.originalPitch = p.getXRot();
        s.taskIndex = idx;
        return s;
    }

    public static class SerializedItem {
        public int slot;
        public String nbt;

        public SerializedItem() {}

        public SerializedItem(int slot, String nbt) {
            this.slot = slot;
            this.nbt = nbt;
        }
    }
}
