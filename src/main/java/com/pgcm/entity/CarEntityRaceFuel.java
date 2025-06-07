package com.pgcm.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class CarEntityRaceFuel extends CarEntity {
    public CarEntityRaceFuel(EntityType<?> type, Level level, CarType carType) {
        super(type, level, carType);
    }

    @Override
    public float getInitialFuel() {
        return 1.0f; // 100%スタート
    }
    @Override
    protected boolean isInfiniteFuel() {
        return false;
    }

    @Override
    protected boolean isTireConsumable() {
        return true;
    }
}
