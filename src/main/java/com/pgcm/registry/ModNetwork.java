package com.pgcm.registry;

import com.pgcm.network.CarControlPacket;
import com.pgcm.network.HeadlightsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel CHANNEL;  // static final を外す

    private static int packetId = 0;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation("pgcm", "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        // CarControlPacketの登録
        CHANNEL.registerMessage(packetId++, CarControlPacket.class,
                CarControlPacket::encode,
                CarControlPacket::decode,
                CarControlPacket::handle);

        // HeadlightsPacketの登録
        CHANNEL.registerMessage(packetId++, HeadlightsPacket.class,
                HeadlightsPacket::toBytes,
                HeadlightsPacket::new,
                HeadlightsPacket::handle);
    }
}
