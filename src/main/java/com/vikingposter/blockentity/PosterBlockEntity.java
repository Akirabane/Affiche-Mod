package com.vikingposter.blockentity;

import com.vikingposter.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PosterBlockEntity extends BlockEntity {

    public static final int  MAX_DEGRADATION       = 5;
    public static final long PLAYER_COOLDOWN_TICKS = 30L * 60 * 20; // 30 min
    public static final long NATURAL_TICK_INTERVAL = 60L * 60 * 20; // 1 h

    private UUID   ownerUUID;
    private String ownerName = "";

    private String imageUrl       = "";
    private float  displayWidth   = 1.0f;
    private float  displayHeight  = 1.0f;
    private float  displayOffsetX = 0.0f;
    private float  displayOffsetY = 0.0f;

    private int degradationLevel = 0;

    // Transient — not persisted. Set by QTE success, consumed by next update from that player.
    private UUID pendingEditor = null;

    // Per-player cooldown: UUID → tick of last successful QTE-degradation by that player.
    private final Map<UUID, Long> playerDegradationCooldowns = new HashMap<>();

    // Tick of last natural (passive) degradation increment. -1 = not initialized.
    private long lastNaturalTick = -1L;

    public PosterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POSTER.get(), pos, state);
    }

    public void setOwnerUUID(UUID uuid)  { this.ownerUUID = uuid; }
    public void setOwnerName(String name){ this.ownerName = name; }
    public @Nullable UUID getOwnerUUID() { return ownerUUID; }
    public String getOwnerName()         { return ownerName; }

    public boolean isOwner(UUID uuid) {
        return ownerUUID == null || ownerUUID.equals(uuid);
    }

    public String getImageUrl()             { return imageUrl; }
    public float  getDisplayWidth()         { return displayWidth; }
    public float  getDisplayHeight()        { return displayHeight; }
    public float  getDisplayOffsetX()       { return displayOffsetX; }
    public float  getDisplayOffsetY()       { return displayOffsetY; }
    public void setImageUrl(String url)     { this.imageUrl = url == null ? "" : url; }
    public void setDisplayWidth(float w)    { this.displayWidth  = Math.max(0.5f, Math.min(16f, w)); }
    public void setDisplayHeight(float h)   { this.displayHeight = Math.max(0.5f, Math.min(16f, h)); }
    public void setDisplayOffsetX(float ox) { this.displayOffsetX = Math.max(-16f, Math.min(16f, ox)); }
    public void setDisplayOffsetY(float oy) { this.displayOffsetY = Math.max(-16f, Math.min(16f, oy)); }

    public int  getDegradationLevel()         { return degradationLevel; }
    public void setDegradationLevel(int lvl)  { this.degradationLevel = Math.max(0, Math.min(MAX_DEGRADATION, lvl)); }
    public boolean isFullyDegraded()          { return degradationLevel >= MAX_DEGRADATION; }
    /** Returns true if the level was incremented; false if already at max. */
    public boolean incrementDegradation() {
        if (degradationLevel >= MAX_DEGRADATION) return false;
        degradationLevel++;
        return true;
    }
    /** Returns true if the level was decremented; false if already at 0. */
    public boolean decrementDegradation() {
        if (degradationLevel <= 0) return false;
        degradationLevel--;
        return true;
    }

    /** Remaining ticks before this player can degrade again, or 0 if available. */
    public long getPlayerCooldownRemaining(UUID uuid, long currentTick) {
        Long last = playerDegradationCooldowns.get(uuid);
        if (last == null) return 0L;
        long elapsed = currentTick - last;
        return elapsed >= PLAYER_COOLDOWN_TICKS ? 0L : PLAYER_COOLDOWN_TICKS - elapsed;
    }

    public void markPlayerDegraded(UUID uuid, long currentTick) {
        playerDegradationCooldowns.put(uuid, currentTick);
    }

    /** Server-tick hook: if 1h passed since the last natural increment, +1 level (capped). Returns true if level changed. */
    public boolean tickNaturalDegradation(long currentTick) {
        if (lastNaturalTick < 0L) { lastNaturalTick = currentTick; return false; }
        if (degradationLevel >= MAX_DEGRADATION) { lastNaturalTick = currentTick; return false; }
        if (currentTick - lastNaturalTick < NATURAL_TICK_INTERVAL) return false;
        lastNaturalTick = currentTick;
        return incrementDegradation();
    }

    public void setPendingEditor(UUID uuid) { this.pendingEditor = uuid; }
    public boolean canEdit(UUID uuid) {
        if (isOwner(uuid)) return true;
        return pendingEditor != null && pendingEditor.equals(uuid);
    }
    public void consumePendingEditor() { this.pendingEditor = null; }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeData(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        readData(tag);
    }

    public CompoundTag saveData() {
        CompoundTag tag = new CompoundTag();
        writeData(tag);
        return tag;
    }

    private void writeData(CompoundTag tag) {
        if (ownerUUID != null) tag.putUUID("owner", ownerUUID);
        tag.putString("ownerName", ownerName);
        tag.putString("imageUrl", imageUrl);
        tag.putFloat("displayWidth", displayWidth);
        tag.putFloat("displayHeight", displayHeight);
        tag.putFloat("displayOffsetX", displayOffsetX);
        tag.putFloat("displayOffsetY", displayOffsetY);
        tag.putInt("degradationLevel", degradationLevel);
        tag.putLong("lastNaturalTick", lastNaturalTick);

        ListTag cooldowns = new ListTag();
        for (Map.Entry<UUID, Long> e : playerDegradationCooldowns.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("uuid", e.getKey());
            entry.putLong("tick", e.getValue());
            cooldowns.add(entry);
        }
        tag.put("playerCooldowns", cooldowns);
    }

    public void readData(CompoundTag tag) {
        ownerUUID      = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        ownerName      = tag.getString("ownerName");
        imageUrl       = tag.getString("imageUrl");
        displayWidth   = tag.getFloat("displayWidth") == 0 ? 1.0f : tag.getFloat("displayWidth");
        displayHeight  = tag.getFloat("displayHeight") == 0 ? 1.0f : tag.getFloat("displayHeight");
        displayOffsetX = tag.contains("displayOffsetX") ? tag.getFloat("displayOffsetX") : 0.0f;
        displayOffsetY = tag.contains("displayOffsetY") ? tag.getFloat("displayOffsetY") : 0.0f;
        // Read int field, with fallback to legacy boolean "degraded" → level 1.
        if (tag.contains("degradationLevel")) {
            degradationLevel = Math.max(0, Math.min(MAX_DEGRADATION, tag.getInt("degradationLevel")));
        } else {
            degradationLevel = tag.getBoolean("degraded") ? 1 : 0;
        }
        lastNaturalTick = tag.contains("lastNaturalTick") ? tag.getLong("lastNaturalTick") : -1L;

        playerDegradationCooldowns.clear();
        ListTag cooldowns = tag.getList("playerCooldowns", Tag.TAG_COMPOUND);
        for (int i = 0; i < cooldowns.size(); i++) {
            CompoundTag entry = cooldowns.getCompound(i);
            if (entry.hasUUID("uuid")) {
                playerDegradationCooldowns.put(entry.getUUID("uuid"), entry.getLong("tick"));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveData();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
                             ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) readData(tag);
    }
}
