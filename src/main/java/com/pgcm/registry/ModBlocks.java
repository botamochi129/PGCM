package com.pgcm.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.pgcm.Pgcm;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Pgcm.MODID);

    public static final RegistryObject<Block> FD3S_BLOCK = BLOCKS.register("fd3s_block", () -> new Block(Block.Properties.of(Material.METAL).strength(2.0F).noOcclusion()));
}