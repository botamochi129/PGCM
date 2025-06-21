package com.pgcm.registry;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.pgcm.Pgcm;
import com.pgcm.registry.ModItems;

@Mod.EventBusSubscriber(modid = Pgcm.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModCreativeTabs {
    public static CreativeModeTab PGCM_TAB;

    @SubscribeEvent
    public static void onCreativeTabRegister(CreativeModeTabEvent.Register event) {
        PGCM_TAB = event.registerCreativeModeTab(
                new net.minecraft.resources.ResourceLocation(Pgcm.MODID, "pgcm_tab"),
                builder -> builder
                        .icon(() -> new ItemStack(ModItems.FD3S_SUMMON_ITEM.get())) // お好きなアイコンに
                        .title(Component.translatable("itemGroup.pgcm_tab"))
                        .displayItems((params, output) -> {
                            output.accept(ModItems.FD3S_SUMMON_ITEM.get());
                            output.accept(ModItems.FD3S_INFINITE_SUMMON_ITEM.get());
                            output.accept(ModItems.CAR_REMOVER_ITEM.get());
                        })
        );
    }
}