package com.qendolin.betterclouds.clouds;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.qendolin.betterclouds.Config;
import com.qendolin.betterclouds.Main;
import com.qendolin.betterclouds.clouds.shaders.Shader;
import com.qendolin.betterclouds.compat.IrisCompat;
import com.qendolin.betterclouds.compat.SodiumExtraCompat;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.List;

import static com.qendolin.betterclouds.Main.glCompat;
import static org.lwjgl.opengl.GL32.*;

public class Renderer implements AutoCloseable {
    private final Minecraft client;
    private ClientLevel world = null;

    private float cloudsHeight;
    private int defaultFbo;
    private final Matrix4f mvpMatrix = new Matrix4f();
    private final Matrix4f rotationProjectionMatrix = new Matrix4f();
    private final Matrix4f tempMatrix = new Matrix4f();
    private final Vector3f tempVector = new Vector3f();
    private final Frustum tempFrustum = new Frustum(new Matrix4f().identity(), new Matrix4f().identity());
    private final PrimitiveChangeDetector shaderInvalidator = new PrimitiveChangeDetector(false);

    private final Resources res = new Resources();

    public Renderer(Minecraft client) {
        this.client = client;
    }

    public void setWorld(ClientLevel world) {
        this.world = world;
    }

    public void reload(ResourceManager manager) {
        Main.LOGGER.info("Reloading cloud renderer...");
        Main.LOGGER.debug("[1/6] Reloading shaders");
        res.reloadShaders(manager);
        Main.LOGGER.debug("[2/6] Reloading generator");
        res.reloadGenerator(isFancyMode());
        Main.LOGGER.debug("[3/6] Reloading textures");
        res.reloadTextures(client);
        Main.LOGGER.debug("[4/6] Reloading primitive meshes");
        res.reloadMeshPrimitives();
        Main.LOGGER.debug("[5/6] Reloading framebuffer");
        res.reloadFramebuffer(scaledFramebufferWidth(), scaledFramebufferHeight());
        Main.LOGGER.debug("[6/6] Reloading timers");
        res.reloadTimer();
        Main.LOGGER.info("Cloud renderer initialized");
    }

    private boolean isFancyMode() {
        return client.options.getCloudsType() == CloudStatus.FANCY;
    }

    private int scaledFramebufferWidth() {
        return (int) (Main.getConfig().preset().upscaleResolutionFactor * client.getMainRenderTarget().width);
    }

    private int scaledFramebufferHeight() {
        return (int) (Main.getConfig().preset().upscaleResolutionFactor * client.getMainRenderTarget().height);
    }

    public boolean prepare(PoseStack matrices, Matrix4f projMat, int ticks, float tickDelta, Vector3d cam) {
        assert RenderSystem.isOnRenderThread();
        client.getProfiler().push("render_setup");
        Config config = Main.getConfig();

        if (res.failedToLoadCritical()) return false;
        if (!config.irisSupport && IrisCompat.IS_LOADED && IrisCompat.isShadersEnabled()) return false;

        DimensionSpecialEffects effects = world.effects();
        if (SodiumExtraCompat.IS_LOADED && effects.skyType() == DimensionSpecialEffects.SkyType.NORMAL) {
            cloudsHeight = SodiumExtraCompat.getCloudsHeight() + config.yOffset;
        } else {
            cloudsHeight = effects.getCloudHeight() + config.yOffset;
        }

        res.generator().bind();
        if (shaderInvalidator.hasChanged(client.options.getCloudsType(), config.blockDistance(),
            config.fadeEdge, config.sizeXZ, config.sizeY, config.writeDepth)) {
            res.reloadShaders(client.getResourceManager());
        }
        res.generator().reallocateIfStale(config, isFancyMode());

        float raininess = Math.max(0.6f * world.getRainLevel(tickDelta), world.getThunderLevel(tickDelta));
        float cloudiness = raininess * 0.3f + 0.5f;

        res.generator().update(cam, ticks+tickDelta, Main.getConfig(), cloudiness);
        if (res.generator().canGenerate() && !res.generator().generating() && !Debug.generatorPause) {
            client.getProfiler().push("generate_clouds");
            res.generator().generate();
            client.getProfiler().push("render_setup");
        }

        if (res.generator().canSwap()) {
            client.getProfiler().push("swap");
            res.generator().swap();
            client.getProfiler().push("render_setup");
        }

        tempMatrix.set(matrices.last().pose());

        matrices.translate(res.generator().renderOriginX(cam.x), cloudsHeight - cam.y, res.generator().renderOriginZ(cam.z));

        rotationProjectionMatrix.set(projMat);
        // This is fixes issue #14, not entirely sure why, but it forces the matrix to be homogenous
        tempMatrix.m30(0);
        tempMatrix.m31(0);
        tempMatrix.m32(0);
        tempMatrix.m33(0);
        tempMatrix.m23(0);
        tempMatrix.m13(0);
        tempMatrix.m03(0);
        rotationProjectionMatrix.mul(tempMatrix);

        mvpMatrix.set(projMat);
        mvpMatrix.mul(matrices.last().pose());

        // TODO: don't do this dynamically
        defaultFbo = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);

        return true;
    }

    // Don't forget to push / pop matrix stack outside
    public void render(int ticks, float tickDelta, Vector3d cam, Vector3d frustumPos, Frustum frustum) {
        // Rendering clouds when underwater was making them very visible in unloaded chunks
        if (client.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE) return;

        client.getProfiler().push("render_setup");
        if (Main.isProfilingEnabled()) {
            if (res.timer() == null) res.reloadTimer();
            res.timer().start();
        }

        Config config = Main.getConfig();

        if (isFramebufferStale()) {
            res.reloadFramebuffer(scaledFramebufferWidth(), scaledFramebufferHeight());
        }

        RenderSystem.viewport(0, 0, res.fboWidth(), res.fboHeight());
        GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, res.oitFbo());
        RenderSystem.clearDepth(1);
        RenderSystem.clearColor(0, 0, 0, 0);

        client.getProfiler().push("draw_depth");
        drawDepth();

        client.getProfiler().push("draw_coverage");
        drawCoverage(ticks + tickDelta, cam, frustumPos, frustum);

        client.getProfiler().push("draw_shading");  //todo test popPush
        if (IrisCompat.IS_LOADED && IrisCompat.isShadersEnabled() && config.useIrisFBO) {
            IrisCompat.bindFramebuffer();
        } else {
            GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, defaultFbo);
        }

        drawShading(tickDelta);

        client.getProfiler().push("render_cleanup");

        res.generator().unbind();
        Shader.unbind();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL_LEQUAL);
        RenderSystem.activeTexture(GL_TEXTURE0);
        RenderSystem.colorMask(true, true, true, true);
        glDisable(GL_STENCIL_TEST);
        glStencilFunc(GL_ALWAYS, 0x0, 0xff);

        if (Debug.frustumCulling) {
            glCompat.pushDebugGroup("Frustum Culling Debug Draw");
            Debug.drawFrustumCulledBoxes(cam);
            glCompat.popDebugGroup();
        }

        if (Main.isProfilingEnabled() && res.timer() != null) {
            res.timer().stop();

            if (res.timer().frames() >= Debug.profileInterval) {
                List<Double> times = res.timer().get();
                times.sort(Double::compare);
                double median = times.get(times.size() / 2);
                double p25 = times.get((int) Math.ceil(times.size() * 0.25));
                double p75 = times.get((int) Math.ceil(times.size() * 0.75));
                double min = times.get(0);
                double max = times.get(times.size() - 1);
                double average = times.stream().mapToDouble(d -> d).average().orElse(0);
                Main.debugChatMessage("profiling.gpuTimes", min, average, max, p25, median, p75);
                res.timer().reset();
            }
        }
    }

    private boolean isFramebufferStale() {
        return res.fboWidth() != scaledFramebufferWidth() || res.fboHeight() != scaledFramebufferHeight();
    }

    private void drawDepth() {
        RenderSystem.disableBlend();
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL_ALWAYS);
        RenderSystem.depthMask(true);

        res.depthShader().bind();

        RenderSystem.activeTexture(GL_TEXTURE0);
        RenderSystem.bindTexture(client.getMainRenderTarget().getDepthTextureId());

        glBindVertexArray(res.cubeVao());
        glDrawArrays(GL_TRIANGLES, 0, Mesh.QUAD_MESH_VERTEX_COUNT);
    }

    private void drawCoverage(float ticks, Vector3d cam, Vector3d frustumPos, Frustum frustum) {
        glEnable(GL_STENCIL_TEST);
        glStencilMask(0xff);
        glStencilOp(GL_KEEP, GL_INCR, GL_INCR);
        glStencilFunc(GL_ALWAYS, 0xff, 0xff);
        RenderSystem.depthFunc(GL_LESS);
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        if(isFancyMode()) RenderSystem.enableCull();
        else RenderSystem.disableCull();
        glClear(GL_STENCIL_BUFFER_BIT | GL_COLOR_BUFFER_BIT);

        Config generatorConfig = getGeneratorConfig();
        Config config = Main.getConfig();

        res.coverageShader().bind();
        res.coverageShader().uMVPMatrix.setMat4(mvpMatrix);
        res.coverageShader().uOriginOffset.setVec3((float) -res.generator().renderOriginX(cam.x), (float) cam.y - cloudsHeight, (float) -res.generator().renderOriginZ(cam.z));
        res.coverageShader().uBoundingBox.setVec4((float) cam.x, (float) cam.z, generatorConfig.blockDistance() - generatorConfig.chunkSize / 2f, generatorConfig.yRange + config.sizeY);
        res.coverageShader().uTime.setFloat(ticks / 20);
        res.coverageShader().uMiscellaneous.setVec2(config.scaleFalloffMin, config.windFactor);

        RenderSystem.activeTexture(GL_TEXTURE5);
        client.getTextureManager().getTexture(Resources.NOISE_TEXTURE).bind();

        res.generator().bind();

        setFrustumTo(tempFrustum, frustum);
        Frustum frustumAtOrigin = tempFrustum;
        frustumAtOrigin.prepare(frustumPos.x - res.generator().originX(), frustumPos.y, frustumPos.z - res.generator().originZ());
        Debug.clearFrustumCulledBoxed();
        if (res.generator().canRender()) {
            int runStart = -1;
            int runCount = 0;
            for (ChunkedGenerator.ChunkIndex chunk : res.generator().chunks()) {
                AABB bounds = chunk.bounds(cloudsHeight, config.sizeXZ, config.sizeY);
                if (!frustumAtOrigin.isVisible(bounds)) {
                    Debug.addFrustumCulledBox(bounds, false);
                    if (runCount != 0) {
                        glCompat.drawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, 0, res.generator().instanceVertexCount(), runCount, runStart);
                    }
                    runStart = -1;
                    runCount = 0;
                } else {
                    Debug.addFrustumCulledBox(bounds, true);
                    if (runStart == -1) runStart = chunk.start();
                    runCount += chunk.count();
                }
            }
            if (runCount != 0) {
                glCompat.drawArraysInstancedBaseInstance(GL_TRIANGLE_STRIP, 0, res.generator().instanceVertexCount(), runCount, runStart);
            }
        }

        RenderSystem.enableCull();
    }

    private void drawShading(float tickDelta) {
        Config config = Main.getConfig();

        if (config.writeDepth) {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL_ALWAYS);
        } else {
            RenderSystem.disableDepthTest();
        }

        RenderSystem.enableBlend();
        RenderSystem.blendEquation(GL_FUNC_ADD);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
        RenderSystem.colorMask(false, false, false, false);
        glColorMaski(0, true, true, true, true);
        glStencilFunc(GL_GREATER, 0x0, 0xff);
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

        RenderSystem.activeTexture(GL_TEXTURE1);
        RenderSystem.bindTexture(res.oitCoverageDepthView());
        RenderSystem.activeTexture(GL_TEXTURE2);
        RenderSystem.bindTexture(res.oitDataTexture());
        RenderSystem.activeTexture(GL_TEXTURE3);
        RenderSystem.bindTexture(res.oitCoverageTexture());
        RenderSystem.activeTexture(GL_TEXTURE4);
        client.getTextureManager().getTexture(Resources.LIGHTING_TEXTURE).bind();

        float effectLuma = getEffectLuminance(tickDelta);
        long skyTime = world.getMoonPhase() % 24000;
        float skyAngleRad = world.getSunAngle(tickDelta);
        float sunPathAngleRad = (float) Math.toRadians(config.preset().sunPathAngle);
        float dayNightFactor = interpolateDayNightFactor(skyTime, config.preset().sunriseStartTime, config.preset().sunriseEndTime, config.preset().sunsetStartTime, config.preset().sunsetEndTime);
        float brightness = (1 - dayNightFactor) * config.preset().nightBrightness + dayNightFactor * config.preset().dayBrightness;
        float sunAxisY = Mth.sin(sunPathAngleRad);
        float sunAxisZ = Mth.cos(sunPathAngleRad);
        Vector3f sunDir = tempVector.set(1, 0, 0).rotateAxis(skyAngleRad + Mth.HALF_PI, 0, sunAxisY, sunAxisZ);

        // TODO: fit light gradient rotation to configured sunset / sunrise values. Solas shader looks weird at sunset
        res.shadingShader().bind();
        res.shadingShader().uVPMatrix.setMat4(rotationProjectionMatrix);
        res.shadingShader().uSunDirection.setVec4(sunDir.x, sunDir.y, sunDir.z, (world.dayTime() % 24000) / 24000f);
        res.shadingShader().uSunAxis.setVec3(0, sunAxisY, sunAxisZ);
        res.shadingShader().uOpacity.setVec3(config.preset().opacity, config.preset().opacityFactor,  config.preset().opacityExponent);
        res.shadingShader().uColorGrading.setVec4(brightness, 1f / config.preset().gamma(), effectLuma, config.preset().saturation);
        res.shadingShader().uTint.setVec3(config.preset().tintRed, config.preset().tintGreen, config.preset().tintBlue);
        res.shadingShader().uNoiseFactor.setFloat(config.colorVariationFactor);


        glBindVertexArray(res.cubeVao());
        glDrawArrays(GL_TRIANGLES, 0, Mesh.CUBE_MESH_VERTEX_COUNT);
    }

    private Config getGeneratorConfig() {
        Config config = res.generator().config();
        if (config != null) return config;
        return Main.getConfig();
    }

    private float getEffectLuminance(float tickDelta) {
        float luma = 1.0f;
        float rain = world.getRainLevel(tickDelta);
        if (rain > 0.0f) {
            float f = rain * 0.95f;
            luma *= (1.0 - f) + f * 0.6f;
        }
        float thunder = world.getThunderLevel(tickDelta);
        if (thunder > 0.0f) {
            float f = thunder * 0.95f;
            luma *= (1.0 - f) + f * 0.2f;
        }
        return luma;
    }

    private float interpolateDayNightFactor(float time, float riseStart, float riseEnd, float setStart, float setEnd) {
        if (time <= 6000 || time > 18000) {
            // sunrise time
            if (time > 18000) time -= 24000;
            return smoothstep(time, riseStart, riseEnd);
        } else {
            // sunset time
            return 1 - smoothstep(time, setStart, setEnd);
        }
    }

    private float smoothstep(float x, float e0, float e1) {
        x = Mth.clamp((x - e0) / (e1 - e0), 0, 1);
        return x * x * (3 - 2 * x);
    }

    private void setFrustumTo(Frustum dst, Frustum src) {
        dst.intersection.set(src.matrix);
        dst.matrix.set(src.matrix);
        dst.camX = src.camX;
        dst.camY = src.camY;
        dst.camZ = src.camZ;
        dst.viewVector = src.viewVector;
    }

    public void close() {
        res.close();
    }
}
