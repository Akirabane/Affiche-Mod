package com.vikingposter.network;

import com.vikingposter.blockentity.PosterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CSyncPosterPacket {

    private final BlockPos pos;
    private final CompoundTag data;

    public S2CSyncPosterPacket(BlockPos pos, CompoundTag data) {
        this.pos  = pos;
        this.data = data;
    }

    public static void encode(S2CSyncPosterPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeNbt(msg.data);
    }

    public static S2CSyncPosterPacket decode(FriendlyByteBuf buf) {
        return new S2CSyncPosterPacket(buf.readBlockPos(), buf.readNbt());
    }

    public static void handle(S2CSyncPosterPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(msg));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(S2CSyncPosterPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.level == null) return;
            BlockEntity be = mc.level.getBlockEntity(msg.pos);
            if (be instanceof PosterBlockEntity poster) {
                poster.readData(msg.data);
            }
        });
    }
}
