package com.pgcm.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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

        // Yaw回転
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-entityYaw + 90));
        // Pitch回転（前後傾き）を追加
        float pitch = entity.level.isClientSide ? entity.clientXRot : entity.getXRot();
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-pitch));
        // Roll回転
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getRollAngle()));

        // 車種ごとにブロックを切り替え
        BlockState state;
        switch (entity.getCarType()) {
            case FD3S -> state = ModBlocks.FD3S_BLOCK.get().defaultBlockState();
            default -> state = ModBlocks.FD3S_BLOCK.get().defaultBlockState();
        }

        if (entity.isHeadlightsOn()) {
            poseStack.pushPose();
            poseStack.translate(1.0, 0.4, 2.2); // 右ライト位置（車モデルに合わせて調整）
            drawHeadlight(poseStack, bufferSource, 0xFFFFFFFF);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(-1.0, 0.4, 2.2); // 左ライト位置
            drawHeadlight(poseStack, bufferSource, 0xFFFFFFFF);
            poseStack.popPose();
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

    private void drawHeadlight(PoseStack poseStack, MultiBufferSource bufferSource, int color) {
        var matrix = poseStack.last().pose();
        var builder = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.translucent());

        float radius = 0.25f;
        int segments = 24;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        float cx = 0f, cy = 0.15f, cz = 0f;
        int light = 0xF000F0; // 最大光源

        for (int i = 0; i < segments; i++) {
            double angle1 = 2 * Math.PI * i / segments;
            double angle2 = 2 * Math.PI * (i + 1) / segments;
            float x1 = cx + (float)Math.cos(angle1) * radius;
            float y1 = cy + (float)Math.sin(angle1) * radius;
            float x2 = cx + (float)Math.cos(angle2) * radius;
            float y2 = cy + (float)Math.sin(angle2) * radius;

            // すべての属性を必ず指定
            builder.vertex(matrix, cx, cy, cz).color(r, g, b, a).uv(0.5f, 0.5f).uv2(light).endVertex();
            builder.vertex(matrix, x1, y1, cz).color(r, g, b, a).uv(
                    0.5f + 0.5f * (float)Math.cos(angle1), 0.5f + 0.5f * (float)Math.sin(angle1)
            ).uv2(light).endVertex();
            builder.vertex(matrix, x2, y2, cz).color(r, g, b, a).uv(
                    0.5f + 0.5f * (float)Math.cos(angle2), 0.5f + 0.5f * (float)Math.sin(angle2)
            ).uv2(light).endVertex();
        }
    }
}
