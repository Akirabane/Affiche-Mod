package com.vikingposter.network;

import com.vikingposter.blockentity.PosterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SQTEResultPacket {

    private final BlockPos pos;
    private final boolean  success;

    public C2SQTEResultPacket(BlockPos pos, boolean success) {
        this.pos     = pos;
        this.success = success;
    }

    public static void encode(C2SQTEResultPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeBoolean(msg.success);
    }

    public static C2SQTEResultPacket decode(FriendlyByteBuf buf) {
        return new C2SQTEResultPacket(buf.readBlockPos(), buf.readBoolean());
    }

    public static void handle(C2SQTEResultPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (!msg.success) return;

            ServerLevel level = player.serverLevel();
            BlockEntity raw = level.getBlockEntity(msg.pos);
            if (!(raw instanceof PosterBlockEntity be)) return;

            // QTE was only ever started for non-fully-degraded posters, but re-check defensively.
            if (!be.incrementDegradation()) return;
            be.markPlayerDegraded(player.getUUID(), level.getGameTime());
            be.setPendingEditor(player.getUUID());
            be.setChanged();
            level.sendBlockUpdated(msg.pos, level.getBlockState(msg.pos),
                level.getBlockState(msg.pos), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            NetworkHandler.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(msg.pos)),
                new S2CSyncPosterPacket(msg.pos, be.saveData()));
            NetworkHandler.sendToClient(new S2COpenEditPacket(msg.pos, be.saveData()), player);
        });
        ctx.get().setPacketHandled(true);
    }
}
