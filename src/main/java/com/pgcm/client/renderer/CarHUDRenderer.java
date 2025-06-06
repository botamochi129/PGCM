package com.pgcm.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.pgcm.entity.CarEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = "pgcm", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CarHUDRenderer {
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!(mc.player.getVehicle() instanceof CarEntity car)) return;

        PoseStack poseStack = event.getPoseStack();

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int zoneTop = (int)(screenHeight * 0.7);
        int zoneHeight = screenHeight - zoneTop;

        int centerX = screenWidth / 2;
        int centerY = zoneTop + zoneHeight / 2;
        int radius = Math.min(zoneHeight / 2 - 10, screenWidth / 6);

        // 半透明背景（より透明に）
        GuiComponent.fill(poseStack, 0, zoneTop, screenWidth, screenHeight, 0x11000000);

        for (int i = 0; i <= 180; i += 20) {
            double angleRad = Math.toRadians(-90 + i);
            double angleRad2 = Math.toRadians(i - 180);

            float angle = -90f + i;
            float cx = (float)(centerX + Math.cos(angleRad2) * (radius - 5));
            float cy = (float)(centerY + Math.sin(angleRad2) * (radius - 5));
            float length = 12;      // 線の長さ
            float thickness = 3;    // 線の太さ
            drawRotatedRect(poseStack, cx, cy, length, thickness, angle, 0xFFFFFFFF);

            // 数字の位置も90度右回転
            int textXorig = (int)(centerX + Math.cos(angleRad) * (radius + 12));
            int textYorig = (int)(centerY + Math.sin(angleRad) * (radius + 12));
            int textX = centerX + (textYorig - centerY);
            int textY = centerY - (textXorig - centerX) + 10;

            String label = String.valueOf(i);
            int textWidth = mc.font.width(label);
            mc.font.drawShadow(poseStack, label, textX - textWidth / 2, textY - 6, 0xFFFFFFFF);
        }

        double speed = car.getDeltaMovement().length() * 72.0;
        float clampedSpeed = (float) Mth.clamp(speed, 0, 180);
        float angleDeg = -90f + (clampedSpeed / 180f) * 180f;

        poseStack.pushPose();
        poseStack.translate(centerX, centerY, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angleDeg));
        drawFilledTriangle(poseStack, 0, -radius + 10, -4, 8, 4, 8, 0xFFFF0000);
        poseStack.popPose();

        String speedText = String.format("%.0f km/h", speed);
        int speedTextWidth = mc.font.width(speedText);
        int speedTextX = centerX - speedTextWidth / 2;
        int speedTextY = zoneTop + zoneHeight - 25;
        mc.font.drawShadow(poseStack, speedText, speedTextX, speedTextY, 0xFFFFFFFF);
    }

    private static void drawFilledTriangle(PoseStack poseStack, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        Matrix4f matrix = poseStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        RenderSystem.setShader(() -> net.minecraft.client.renderer.GameRenderer.getPositionColorShader());

        float a = (color >> 24 & 0xFF) / 255f;
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x1, y1, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x3, y3, 0).color(r, g, b, a).endVertex();
        Tesselator.getInstance().end();
    }

    private static void drawRotatedRect(PoseStack poseStack, float cx, float cy, float length, float thickness, float angleDeg, int color) {
        poseStack.pushPose();
        poseStack.translate(cx, cy, 0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angleDeg));
        // 矩形の左上・右下（中心からY軸方向にlength/2, X軸方向にthickness/2）
        float x1 = -thickness / 2f;
        float y1 = -length / 2f;
        float x2 = thickness / 2f;
        float y2 = length / 2f;
        GuiComponent.fill(poseStack, (int)x1, (int)y1, (int)x2, (int)y2, color);
        poseStack.popPose();
    }
}
