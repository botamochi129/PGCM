package com.pgcm.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class CarEntity extends Entity {
    public enum CarType {FD3S, CAR2 }
    private final CarType carType;

    public CarEntity(EntityType<?> type, Level level, CarType carType) {
        super(type, level);
        this.carType = carType;
    }

    public CarType getCarType() {
        return carType;
    }

    @Override
    protected void defineSynchedData() {}
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}