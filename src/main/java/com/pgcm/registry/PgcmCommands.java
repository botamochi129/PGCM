package com.pgcm.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.pgcm.entity.CarEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

@Mod.EventBusSubscriber(modid = "pgcm")
public class PgcmCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("pgcm")
                        // /pgcm fuel max
                        .then(Commands.literal("fuel")
                                .then(Commands.literal("max")
                                        .executes(ctx -> {
                                            Player player = ctx.getSource().getPlayerOrException();
                                            if (player.getVehicle() instanceof CarEntity car) {
                                                car.fuel = car.getInitialFuel();
                                                car.outOfFuel = false;
                                                car.getEntityData().set(CarEntity.DATA_FUEL, car.fuel);
                                                ctx.getSource().sendSuccess(Component.translatable("pgcm.command.fuel_max.success"), false);
                                                return 1;
                                            } else {
                                                ctx.getSource().sendFailure(Component.translatable("pgcm.command.not_in_car"));
                                                return 0;
                                            }
                                        })
                                )
                                // /pgcm fuel set <value>
                                .then(Commands.literal("set")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                                .executes(ctx -> {
                                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                                    Player player = ctx.getSource().getPlayerOrException();
                                                    if (player.getVehicle() instanceof CarEntity car) {
                                                        car.fuel = car.getInitialFuel() * (value / 100.0f);
                                                        car.outOfFuel = (car.fuel <= 0);
                                                        car.getEntityData().set(CarEntity.DATA_FUEL, car.fuel);
                                                        ctx.getSource().sendSuccess(Component.translatable("pgcm.command.fuel_set.success", value), false);
                                                        return 1;
                                                    } else {
                                                        ctx.getSource().sendFailure(Component.translatable("pgcm.command.not_in_car"));
                                                        return 0;
                                                    }
                                                })
                                        )
                                )
                        )
                        // /pgcm tire reset
                        .then(Commands.literal("tire")
                                .then(Commands.literal("reset")
                                        .executes(ctx -> {
                                            Player player = ctx.getSource().getPlayerOrException();
                                            if (player.getVehicle() instanceof CarEntity car) {
                                                car.tireWear = 1.0f;
                                                car.tireWornOut = false;
                                                car.getEntityData().set(CarEntity.DATA_TIRE_WEAR, car.tireWear);
                                                ctx.getSource().sendSuccess(Component.translatable("pgcm.command.tire_reset.success"), false);
                                                return 1;
                                            } else {
                                                ctx.getSource().sendFailure(Component.translatable("pgcm.command.not_in_car"));
                                                return 0;
                                            }
                                        })
                                )
                                // /pgcm tire set <value>
                                .then(Commands.literal("set")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 100))
                                                .executes(ctx -> {
                                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                                    Player player = ctx.getSource().getPlayerOrException();
                                                    if (player.getVehicle() instanceof CarEntity car) {
                                                        car.tireWear = value / 100.0f;
                                                        car.tireWornOut = car.tireWear <= 0.0f;
                                                        car.getEntityData().set(CarEntity.DATA_TIRE_WEAR, car.tireWear);
                                                        ctx.getSource().sendSuccess(Component.translatable("pgcm.command.tire_set.success", value), false);
                                                        return 1;
                                                    } else {
                                                        ctx.getSource().sendFailure(Component.translatable("pgcm.command.not_in_car"));
                                                        return 0;
                                                    }
                                                })
                                        )
                                )
                        )
        );
    }
}
