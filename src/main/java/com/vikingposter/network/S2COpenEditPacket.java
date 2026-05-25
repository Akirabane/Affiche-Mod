package com.vikingposter.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2COpenEditPacket {

    private final BlockPos pos;
    private final CompoundTag data;

    public S2COpenEditPacket(BlockPos pos, CompoundTag data) {
        this.pos  = pos;
        this.data = data;
    }

    public static void encode(S2COpenEditPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeNbt(msg.data);
    }

    public static S2COpenEditPacket decode(FriendlyByteBuf buf) {
        return new S2COpenEditPacket(buf.readBlockPos(), buf.readNbt());
    }

    public static void handle(S2COpenEditPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(msg));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(S2COpenEditPacket msg) {
        Minecraft.getInstance().execute(() ->
            com.vikingposter.client.web.PosterEditScreen.open(msg.pos, msg.data));
    }
}
