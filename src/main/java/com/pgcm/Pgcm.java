package com.pgcm;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.pgcm.registry.ModBlocks;
import com.pgcm.registry.ModEntities;

@Mod(Pgcm.MODID)
public class Pgcm {
    public static final String MODID = "pgcm";
    public Pgcm() {
        ModBlocks.BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ModEntities.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}