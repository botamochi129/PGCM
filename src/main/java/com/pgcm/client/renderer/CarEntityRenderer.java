package com.pgcm.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.pgcm.Pgcm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import com.pgcm.entity.CarEntity;
import com.pgcm.registry.ModBlocks;

public class CarEntityRenderer extends EntityRenderer<CarEntity> {
    public CarEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    private static final ResourceLocation WHITE_TEX = new ResourceLocation(Pgcm.MODID, "textures/misc/white.png");

    @Override
    public void render(CarEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        //Yaw
        float smoothYaw = entity.clientYRot;
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-smoothYaw + 90));
        // Pitch回転（前後傾き）もsmoothed値で
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

        if (entity.brakeLampOn || entity.tailLampOn) {
            poseStack.pushPose();
            poseStack.translate(2.5, 1.15, 1.1); // 左後ろ（調整推奨）
            drawLamp(poseStack, bufferSource, 0xFFFF2222, 0.10f); // 赤
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(2.5, 1.15, -1.1); // 右後ろ
            drawLamp(poseStack, bufferSource, 0xFFFF2222, 0.10f);
            poseStack.popPose();
        }

        // バックランプ（後部中央寄り左右）
        if (entity.backLampOn) {
            poseStack.pushPose();
            poseStack.translate(2.5, 1.15, 0.5); // 左寄り
            drawLamp(poseStack, bufferSource, 0xFFFFFFFF, 0.08f); // 白
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(2.5, 1.15, -0.5); // 右寄り
            drawLamp(poseStack, bufferSource, 0xFFFFFFFF, 0.08f);
            poseStack.popPose();
        }

        // ウインカー（後部左右端・点滅）
        boolean blink = (entity.tickCount / 10) % 2 == 0; // 0.5秒ごと点滅
        if (entity.leftBlinkerOn && blink) {
            poseStack.pushPose();
            poseStack.translate(2.5, 1.15, 1.25); // 左端
            drawLamp(poseStack, bufferSource, 0xFFFFA500, 0.08f); // 橙
            poseStack.popPose();
        }
        if (entity.rightBlinkerOn && blink) {
            poseStack.pushPose();
            poseStack.translate(2.5, 1.15, -1.25); // 右端
            drawLamp(poseStack, bufferSource, 0xFFFFA500, 0.08f);
            poseStack.popPose();
        }

        // 前部ウインカー（フロント左右端）
        if (entity.leftBlinkerOn && blink) {
            poseStack.pushPose();
            poseStack.translate(-4.15, 0.8, 1.1); // 左前
            drawLamp(poseStack, bufferSource, 0xFFFFA500, 0.08f);
            poseStack.popPose();
        }
        if (entity.rightBlinkerOn && blink) {
            poseStack.pushPose();
            poseStack.translate(-4.15, 0.8, -1.1); // 右前
            drawLamp(poseStack, bufferSource, 0xFFFFA500, 0.08f);
            poseStack.popPose();
        }

        if (entity.isHeadlightsOn()) {
            poseStack.pushPose();
            poseStack.translate(-4.0, 0.75, 0.9); // 右ライト位置（車モデルに合わせて調整）
            drawLamp(poseStack, bufferSource, 0xFFFFFFFF, 0.09f);
            poseStack.popPose();

            poseStack.pushPose();
            poseStack.translate(-4.0, 0.75, -0.9); // 左ライト位置
            drawLamp(poseStack, bufferSource, 0xFFFFFFFF, 0.09f);
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

    private void drawLamp(PoseStack poseStack, MultiBufferSource bufferSource, int color, float radius) {
        var matrix = poseStack.last().pose();
        var normalMatrix = poseStack.last().normal();
        var builder = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucent(WHITE_TEX));

        int segments = 32;
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float cx = 0f, cy = 0f, cz = 0f;
        int light = 0xF000F0;
        float nx = 1, ny = 0, nz = 0;

        for (int i = 0; i < segments; i++) {
            double angle1 = 2 * Math.PI * i / segments;
            double angle2 = 2 * Math.PI * (i + 1) / segments;
            float y1 = cy + (float) Math.cos(angle1) * radius;
            float z1 = cz + (float) Math.sin(angle1) * radius;
            float y2 = cy + (float) Math.cos(angle2) * radius;
            float z2 = cz + (float) Math.sin(angle2) * radius;

            builder.vertex(matrix, cx, cy, cz)
                    .color(r, g, b, a)
                    .uv(0.5f, 0.5f)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light)
                    .normal(normalMatrix, nx, ny, nz)
                    .endVertex();

            builder.vertex(matrix, cx, y2, z2)
                    .color(r, g, b, a)
                    .uv(0.5f, 0.5f)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light)
                    .normal(normalMatrix, nx, ny, nz)
                    .endVertex();

            builder.vertex(matrix, cx, y1, z1)
                    .color(r, g, b, a)
                    .uv(0.5f, 0.5f)
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light)
                    .normal(normalMatrix, nx, ny, nz)
                    .endVertex();
        }
    }
}
