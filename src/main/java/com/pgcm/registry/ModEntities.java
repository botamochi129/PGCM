package com.pgcm.registry;

import com.pgcm.entity.CarEntityInfiniteFuel;
import com.pgcm.entity.CarEntityRaceFuel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.pgcm.Pgcm;
import com.pgcm.entity.CarEntity;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Pgcm.MODID);

    // 複数車種例
    public static final RegistryObject<EntityType<CarEntityInfiniteFuel>> FD3S =
            ENTITIES.register("fd3s_infinite", () -> EntityType.Builder.<CarEntityInfiniteFuel>of((type, level) -> new CarEntityInfiniteFuel(type, level, CarEntityInfiniteFuel.CarType.FD3S), MobCategory.MISC)
                    .sized(3.0f, 2.0f)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("fd3s_infinite"));

    public static final RegistryObject<EntityType<CarEntityRaceFuel>> FD3S_RACE =
            ENTITIES.register("fd3s", () -> EntityType.Builder.<CarEntityRaceFuel>of((type, level) -> new CarEntityRaceFuel(type, level, CarEntityRaceFuel.CarType.FD3S), MobCategory.MISC)
                    .sized(3.0f, 2.0f)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("fd3s"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}