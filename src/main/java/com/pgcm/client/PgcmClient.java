package com.pgcm.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.pgcm.client.renderer.CarEntityRenderer;
import com.pgcm.registry.ModEntities;

@Mod.EventBusSubscriber(modid = "pgcm", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PgcmClient {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.FD3S.get(), CarEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.CAR2.get(), CarEntityRenderer::new);
    }
}