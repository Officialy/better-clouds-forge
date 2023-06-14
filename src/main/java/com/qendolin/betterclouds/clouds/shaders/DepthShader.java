package com.qendolin.betterclouds.clouds.shaders;

import com.qendolin.betterclouds.Main;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.Map;

public class DepthShader extends Shader {
    public static final ResourceLocation VERTEX_SHADER_ID = new ResourceLocation(Main.MODID, "shaders/core/betterclouds_depth.vsh");
    public static final ResourceLocation FRAGMENT_SHADER_ID = new ResourceLocation(Main.MODID, "shaders/core/betterclouds_depth.fsh");

    public final Uniform uDepthTexture;

    public DepthShader(ResourceManager resMan, Map<String, String> defs) throws IOException {
        super(resMan, VERTEX_SHADER_ID, FRAGMENT_SHADER_ID, defs);

        uDepthTexture = getUniform("u_depth_texture", false);
    }

    public static DepthShader create(ResourceManager manager) throws IOException {
        return new DepthShader(manager, Map.of());
    }
}
