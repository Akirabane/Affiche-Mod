package com.vikingposter.network;

import com.vikingposter.blockentity.PosterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class C2SUpdatePosterPacket {

    private final BlockPos pos;
    private final String   imageUrl;
    private final float    displayWidth;
    private final float    displayHeight;
    private final float    displayOffsetX;
    private final float    displayOffsetY;

    public C2SUpdatePosterPacket(BlockPos pos,
                                 String imageUrl,
                                 float displayWidth, float displayHeight,
                                 float displayOffsetX, float displayOffsetY) {
        this.pos            = pos;
        this.imageUrl       = imageUrl;
        this.displayWidth   = displayWidth;
        this.displayHeight  = displayHeight;
        this.displayOffsetX = displayOffsetX;
        this.displayOffsetY = displayOffsetY;
    }

    public static void encode(C2SUpdatePosterPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.imageUrl);
        buf.writeFloat(msg.displayWidth);
        buf.writeFloat(msg.displayHeight);
        buf.writeFloat(msg.displayOffsetX);
        buf.writeFloat(msg.displayOffsetY);
    }

    public static C2SUpdatePosterPacket decode(FriendlyByteBuf buf) {
        return new C2SUpdatePosterPacket(
            buf.readBlockPos(), buf.readUtf(),
            buf.readFloat(), buf.readFloat(),
            buf.readFloat(), buf.readFloat());
    }

    public static void handle(C2SUpdatePosterPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            BlockEntity raw = level.getBlockEntity(msg.pos);
            if (!(raw instanceof PosterBlockEntity be)) return;
            if (!be.canEdit(player.getUUID())) return;
            be.consumePendingEditor();

            be.setImageUrl(msg.imageUrl);
            be.setDisplayWidth(msg.displayWidth);
            be.setDisplayHeight(msg.displayHeight);
            be.setDisplayOffsetX(msg.displayOffsetX);
            be.setDisplayOffsetY(msg.displayOffsetY);
            be.setChanged();

            level.sendBlockUpdated(msg.pos, level.getBlockState(msg.pos),
                level.getBlockState(msg.pos), Block.UPDATE_CLIENTS);
            NetworkHandler.CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(msg.pos)),
                new S2CSyncPosterPacket(msg.pos, be.saveData()));
        });
        ctx.get().setPacketHandled(true);
    }
}
