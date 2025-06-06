package com.pgcm.network;

import com.pgcm.entity.CarEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CarControlPacket {
    private final boolean forward;
    private final boolean back;
    private final boolean left;
    private final boolean right;
    private final boolean drift;

    public CarControlPacket(boolean forward, boolean back, boolean left, boolean right, boolean drift) {
        this.forward = forward;
        this.back = back;
        this.left = left;
        this.right = right;
        this.drift = drift;
    }

    // エンコード（送信時）
    public static void encode(CarControlPacket pkt, FriendlyByteBuf buf) {
        buf.writeBoolean(pkt.forward);
        buf.writeBoolean(pkt.back);
        buf.writeBoolean(pkt.left);
        buf.writeBoolean(pkt.right);
        buf.writeBoolean(pkt.drift);
    }

    // デコード（受信時）
    public static CarControlPacket decode(FriendlyByteBuf buf) {
        boolean forward = buf.readBoolean();
        boolean back = buf.readBoolean();
        boolean left = buf.readBoolean();
        boolean right = buf.readBoolean();
        boolean drift = buf.readBoolean();
        return new CarControlPacket(forward, back, left, right, drift);
    }

    // 受信処理
    public static void handle(CarControlPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // サーバースレッドでの処理
            var sender = ctx.get().getSender();
            if (sender != null) {
                // プレイヤーが乗っているCarEntityを探す（複数車がある想定）
                var vehicle = sender.getVehicle();
                if (vehicle instanceof CarEntity car) {
                    car.setControlState(pkt.forward, pkt.back, pkt.left, pkt.right, pkt.drift);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
