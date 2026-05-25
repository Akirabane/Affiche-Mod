package com.vikingposter.network;

import com.vikingposter.VikingPosterMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class NetworkHandler {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath(VikingPosterMod.MODID, "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );

    private static int id = 0;

    public static void init() {
        // Server → Client
        CHANNEL.registerMessage(id++, S2COpenEditPacket.class,
            S2COpenEditPacket::encode, S2COpenEditPacket::decode, S2COpenEditPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2COpenQTEPacket.class,
            S2COpenQTEPacket::encode, S2COpenQTEPacket::decode, S2COpenQTEPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, S2CSyncPosterPacket.class,
            S2CSyncPosterPacket::encode, S2CSyncPosterPacket::decode, S2CSyncPosterPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // Client → Server
        CHANNEL.registerMessage(id++, C2SUpdatePosterPacket.class,
            C2SUpdatePosterPacket::encode, C2SUpdatePosterPacket::decode, C2SUpdatePosterPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, C2SQTEResultPacket.class,
            C2SQTEResultPacket::encode, C2SQTEResultPacket::decode, C2SQTEResultPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendToClient(Object packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}
