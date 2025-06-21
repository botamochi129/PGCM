package com.pgcm;

import com.pgcm.client.PgcmClientModBus;
import com.pgcm.registry.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Pgcm.MODID)
public class Pgcm {
    public static final String MODID = "pgcm";
    public Pgcm() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModEntities.register(bus);
        ModItems.ITEMS.register(bus);
        ModNetwork.register();
    }
}