package com.pgcm.registry;

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
    public static final RegistryObject<EntityType<CarEntity>> FD3S =
            ENTITIES.register("fd3s", () -> EntityType.Builder.<CarEntity>of((type, level) -> new CarEntity(type, level, CarEntity.CarType.FD3S), MobCategory.MISC)
                    .sized(1.5f, 1.5f)
                    .build("fd3s"));

    public static final RegistryObject<EntityType<CarEntity>> CAR2 =
            ENTITIES.register("car2", () -> EntityType.Builder.<CarEntity>of((type, level) -> new CarEntity(type, level, CarEntity.CarType.CAR2), MobCategory.MISC)
                    .sized(1.7f, 1.2f)
                    .build("car2"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}