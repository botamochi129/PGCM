package com.pgcm.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.RegistryObject;

public class FD3SSummonItem<T extends Entity> extends Item {
    private final RegistryObject<EntityType<T>> carType;

    public FD3SSummonItem(RegistryObject<EntityType<T>> carType, Properties properties) {
        super(properties);
        this.carType = carType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            var pos = context.getClickedPos().relative(context.getClickedFace());
            var car = carType.get().create(level);
            if (car != null) {
                car.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0, 0);
                level.addFreshEntity(car);
                if (!context.getPlayer().isCreative()) {
                    context.getItemInHand().shrink(1);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
