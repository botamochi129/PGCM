package com.pgcm;

import com.pgcm.client.PgcmClientModBus;
import com.pgcm.registry.ModBlocks;
import com.pgcm.registry.ModEntities;
import com.pgcm.registry.ModNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Pgcm.MODID)
public class Pgcm {
    public static final String MODID = "pgcm";
    public Pgcm() {
        ModBlocks.BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModEntities.register(FMLJavaModLoadingContext.get().getModEventBus());
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModNetwork.register();
    }
}