package com.pgcm.network;

import com.pgcm.entity.CarEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class HeadlightsPacket {
    public final int entityId;
    public final boolean on;

    public HeadlightsPacket(int entityId, boolean on) {
        this.entityId = entityId;
        this.on = on;
    }

    public HeadlightsPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.on = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(on);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) {
                var entity = player.level.getEntity(entityId);
                if (entity instanceof CarEntity car) {
                    car.setHeadlightsOn(on);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
