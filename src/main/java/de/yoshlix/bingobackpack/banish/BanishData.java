package de.yoshlix.bingobackpack.banish;

import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

public class BanishData {
    public UUID playerId;
    public String originalDimension;
    public double originalX, originalY, originalZ;
    public float originalYaw, originalPitch;
    public int taskIndex;
    public double taskSpawnX, taskSpawnY, taskSpawnZ;
    public int taskTime = 0;

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
}
