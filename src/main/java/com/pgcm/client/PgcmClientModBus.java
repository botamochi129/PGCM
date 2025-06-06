package com.pgcm.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pgcm.client.renderer.CarEntityRenderer;
import com.pgcm.entity.CarEntity;
import com.pgcm.network.HeadlightsPacket;
import com.pgcm.registry.ModEntities;
import com.pgcm.registry.ModNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "pgcm", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PgcmClientModBus {
    public static final KeyMapping KEY_HEADLIGHTS = new KeyMapping(
            "key.pgcm.headlights",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            "key.categories.vehicle"
    );

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FD3S.get(), CarEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY_HEADLIGHTS);
    }

    @Mod.EventBusSubscriber(modid = "pgcm", value = Dist.CLIENT)
    public static class ForgeClientEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && KEY_HEADLIGHTS.consumeClick()) {
                if (mc.player.getVehicle() instanceof CarEntity car) {
                    // サーバーにライトON/OFFのパケット送信
                    ModNetwork.CHANNEL.sendToServer(
                            new HeadlightsPacket(car.getId(), !car.isHeadlightsOn())
                    );
                }
            }
        }
    }
}
