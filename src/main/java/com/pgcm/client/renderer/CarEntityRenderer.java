package com.pgcm.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import com.pgcm.entity.CarEntity;
import com.pgcm.registry.ModBlocks;

public class CarEntityRenderer extends EntityRenderer<CarEntity> {
    public CarEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(CarEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0, 0.6875, 0.0); // 必要に応じて高さ調整
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entityYaw));

        // 車種ごとにブロックを切り替え
        BlockState state;
        switch (entity.getCarType()) {
            case CAR2 -> state = ModBlocks.CAR2_BLOCK.get().defaultBlockState();
            case FD3S -> state = ModBlocks.FD3S_BLOCK.get().defaultBlockState();
            default -> state = ModBlocks.FD3S_BLOCK.get().defaultBlockState();
        }

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        dispatcher.renderSingleBlock(state, poseStack, bufferSource, packedLight, 0);

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CarEntity entity) {
        // ブロックモデルを使う場合はnullでOK
        return null;
    }
}