package com.qendolin.betterclouds.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.qendolin.betterclouds.Main;
import com.qendolin.betterclouds.clouds.Debug;
import com.qendolin.betterclouds.clouds.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL32;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.qendolin.betterclouds.Main.glCompat;

@Mixin(LevelRenderer.class)
public abstract class WorldRendererMixin {

    private final Vector3d tempVector = new Vector3d();

    private Renderer cloudRenderer;
    @Shadow
    private Frustum cullingFrustum;

    private double profTimeAcc;
    private int profFrames;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(Minecraft client, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers bufferBuilders, CallbackInfo ci) {
        if (glCompat.isIncompatible()) return;
        cloudRenderer = new Renderer(client);
    }

    @Shadow
    private @Nullable Frustum capturedFrustum;

    @Shadow
    @Final
    private Vector3d frustumPos;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private @Nullable ClientLevel level;

    @Shadow
    private int ticks;

    @Inject(at = @At("TAIL"), method = "onResourceManagerReload")
    private void onReload(ResourceManager manager, CallbackInfo ci) {
        if (glCompat.isIncompatible()) return;
        if (cloudRenderer != null) cloudRenderer.reload(manager);
    }

    @Inject(at = @At("TAIL"), method = "setLevel")
    private void onSetWorld(ClientLevel world, CallbackInfo ci) {
        if (cloudRenderer != null) cloudRenderer.setWorld(world);
    }

    @Inject(at = @At("HEAD"), method = "renderClouds", cancellable = true)
    private void renderClouds(PoseStack matrices, Matrix4f projMat, float tickDelta, double camX, double camY, double camZ, CallbackInfo ci) {
        if (cloudRenderer == null) return;
        if (glCompat.isIncompatible()) return;
        if (level == null || !level.dimensionTypeRegistration().is(BuiltinDimensionTypes.OVERWORLD)) return;
        if (!Main.getConfig().enabled) return;

        minecraft.getProfiler().push(Main.MODID);
        glCompat.pushDebugGroup("Better Clouds");

        Vector3d cam = tempVector.set(camX, camY, camZ);
        Frustum frustum = this.cullingFrustum;
        Vector3d frustumPos = cam;
        if (capturedFrustum != null) {
            frustum = capturedFrustum;
            frustum.prepare(this.frustumPos.x, this.frustumPos.y, this.frustumPos.z);
            frustumPos = this.frustumPos;
        }

        if (Main.isProfilingEnabled()) GL32.glFinish();
        long startTime = System.nanoTime();

        matrices.pushPose();
        if (cloudRenderer.prepare(matrices, projMat, ticks, tickDelta, cam)) {
            ci.cancel();
            cloudRenderer.render(ticks, tickDelta, cam, frustumPos, frustum);
        }
        matrices.popPose();

        if (Main.isProfilingEnabled()) {
            GL32.glFinish();
            profTimeAcc += (System.nanoTime() - startTime) / 1e6;
            profFrames++;
            if (profFrames >= Debug.profileInterval) {
                Main.debugChatMessage("profiling.cpuTimes", profTimeAcc / profFrames);
                profFrames = 0;
                profTimeAcc = 0;
            }
        }

        minecraft.getProfiler().pop();
        glCompat.popDebugGroup();
    }


    @Inject(at = @At("HEAD"), method = "close")
    private void close(CallbackInfo ci) {
        if (cloudRenderer != null) cloudRenderer.close();
    }
}
