package com.pgcm.registry;

import com.pgcm.item.CarRemoverItem;
import com.pgcm.item.FD3SInfiniteSummonItem;
import com.pgcm.item.FD3SSummonItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "pgcm");

    public static final RegistryObject<Item> FD3S_SUMMON_ITEM =
            ITEMS.register("fd3s_summon", () ->
                    new FD3SSummonItem<>(ModEntities.FD3S_RACE, new Item.Properties())
            );

    public static final RegistryObject<Item> FD3S_INFINITE_SUMMON_ITEM =
            ITEMS.register("fd3s_infinite_summon", () ->
                    new FD3SInfiniteSummonItem<>(ModEntities.FD3S, new Item.Properties())
            );

    public static final RegistryObject<Item> CAR_REMOVER_ITEM =
            ITEMS.register("car_remover", () ->
                    new CarRemoverItem(new Item.Properties())
            );
}
