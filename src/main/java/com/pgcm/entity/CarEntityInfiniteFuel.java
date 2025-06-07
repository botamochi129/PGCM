package com.pgcm.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class CarEntityInfiniteFuel extends CarEntity {
    public CarEntityInfiniteFuel(EntityType<?> type, Level level, CarType carType) {
        super(type, level, carType);
    }

    @Override
    public float getInitialFuel() {
        return 1.0f;
    }
    @Override
    protected boolean isInfiniteFuel() {
        return true;
    }

    @Override
    protected boolean isTireConsumable() {
        return false;
    }
}
