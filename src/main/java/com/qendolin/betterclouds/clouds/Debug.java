package com.qendolin.betterclouds.clouds;

import com.mojang.blaze3d.shaders.Shader;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class Debug {
    public static int profileInterval = 0;
    public static boolean frustumCulling = false;
    public static boolean generatorPause = false;

    public static final List<Pair<AABB, Boolean>> frustumCulledBoxes = new ArrayList<>();

    public static void clearFrustumCulledBoxed() {
        if(frustumCulling) {
            frustumCulledBoxes.clear();
        } else if(!frustumCulledBoxes.isEmpty()) {
            frustumCulledBoxes.clear();
        }
    }

    public static void addFrustumCulledBox(AABB box, boolean visible) {
        if(!frustumCulling) return;
        frustumCulledBoxes.add(Pair.of(box, visible));
    }

    public static void drawFrustumCulledBoxes(Vector3d cam) {
        if(!frustumCulling) return;
        BufferBuilder vertices = Tesselator.getInstance().getBuilder();
        vertices.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        ShaderInstance prevShader = RenderSystem.getShader();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        for (Pair<AABB, Boolean> pair : frustumCulledBoxes) {
            AABB box = pair.getLeft();
            if(pair.getRight()) {
                drawBox(cam, vertices, box, 0.6f, 1f, 0.5f, 1f);
            } else {
                drawBox(cam, vertices, box, 1f, 0.6f, 0.5f, 1f);
            }
        }
        Tesselator.getInstance().end();
        RenderSystem.setShader(() -> prevShader);
    }

    public static void drawBox(Vector3d cam, VertexConsumer vertexConsumer, AABB box, float red, float green, float blue, float alpha) {
        // I was having some issues with WorldRenderer#drawBox and sodium, so I've copied a modified version here
        float minX = (float) (box.minX - cam.x);
        float minY = (float) (box.minY - cam.y);
        float minZ = (float) (box.minZ - cam.z);
        float maxX = (float) (box.maxX - cam.x);
        float maxY = (float) (box.maxY - cam.y);
        float maxZ = (float) (box.maxZ - cam.z);
        vertexConsumer.vertex(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(minX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        vertexConsumer.vertex(maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
    }
}
