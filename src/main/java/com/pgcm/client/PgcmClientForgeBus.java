package com.pgcm.client;

import com.pgcm.network.CarControlPacket;
import com.pgcm.registry.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.pgcm.client.renderer.CarEntityRenderer;
import com.pgcm.registry.ModEntities;

@Mod.EventBusSubscriber(modid = "pgcm", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class PgcmClientForgeBus {
    private static boolean lastForward, lastBack, lastLeft, lastRight, lastDrift;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var options = mc.options;
        boolean forward = options.keyUp.isDown();
        boolean back = options.keyDown.isDown();
        boolean left = options.keyLeft.isDown();
        boolean right = options.keyRight.isDown();
        boolean drift = options.keyJump.isDown();

        if (forward != lastForward || back != lastBack || left != lastLeft || right != lastRight || drift != lastDrift) {
            lastForward = forward;
            lastBack = back;
            lastLeft = left;
            lastRight = right;
            lastDrift = drift;

            ModNetwork.CHANNEL.sendToServer(new CarControlPacket(forward, back, left, right, drift));
        }
    }
}
