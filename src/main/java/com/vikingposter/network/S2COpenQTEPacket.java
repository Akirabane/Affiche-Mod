package com.vikingposter.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2COpenQTEPacket {

    private final BlockPos pos;
    private final int      currentLevel;

    public S2COpenQTEPacket(BlockPos pos, int currentLevel) {
        this.pos          = pos;
        this.currentLevel = currentLevel;
    }

    public static void encode(S2COpenQTEPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.currentLevel);
    }

    public static S2COpenQTEPacket decode(FriendlyByteBuf buf) {
        return new S2COpenQTEPacket(buf.readBlockPos(), buf.readInt());
    }

    public static void handle(S2COpenQTEPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(msg));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(S2COpenQTEPacket msg) {
        Minecraft.getInstance().execute(() ->
            Minecraft.getInstance().setScreen(new com.vikingposter.qte.QTEScreen(msg.pos, msg.currentLevel)));
    }
}
