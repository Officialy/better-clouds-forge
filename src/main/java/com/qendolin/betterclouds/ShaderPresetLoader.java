package com.qendolin.betterclouds;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.BufferedReader;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ShaderPresetLoader extends SimplePreparableReloadListener<Map<String, Config.ShaderConfigPreset>> {
    private static final Gson GSON = new GsonBuilder()
        .setLenient()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(Config.ShaderConfigPreset.class, Config.ShaderConfigPreset.INSTANCE_CREATOR)
        .create();
    public static final ResourceLocation ID = new ResourceLocation(Main.MODID, "shader_presets");
    public static final ResourceLocation RESOURCE_ID = new ResourceLocation(Main.MODID, "betterclouds/shader_presets.json");
    public static final ShaderPresetLoader INSTANCE = new ShaderPresetLoader();

    private Map<String, Config.ShaderConfigPreset> presets = null;

    public Map<String, Config.ShaderConfigPreset> presets() {
        if (presets == null) return Map.of();
        return ImmutableMap.copyOf(presets);
    }

    @Override
    public String getName() {
        return ID.toString();
    }

    @Override
    protected Map<String, Config.ShaderConfigPreset> prepare(ResourceManager manager, ProfilerFiller p_10797_) {
        return (Map<String, Config.ShaderConfigPreset>) CompletableFuture.supplyAsync(() -> {
            Map<String, Config.ShaderConfigPreset> mergedPresets = new HashMap<>();
            Type mapType = new TypeToken<Map<String, Config.ShaderConfigPreset>>() {
            }.getType();
            for (Resource resource : manager.getResourceStack(RESOURCE_ID)) {
                try (BufferedReader reader = resource.openAsReader()) {
                    Map<String, Config.ShaderConfigPreset> presets = GSON.fromJson(reader, mapType);
                    if (presets == null) continue;
                    mergedPresets.putAll(presets);
                } catch (Exception exception) {
                    Main.LOGGER.warn("Failed to parse shader presets {} in pack {}", RESOURCE_ID, resource.sourcePackId(), exception);
                }
            }

            mergedPresets.values().removeAll(Collections.singleton(null));

            for (Map.Entry<String, Config.ShaderConfigPreset> entry : mergedPresets.entrySet()) {
                entry.getValue().editable = false;
                entry.getValue().key = entry.getKey();
            }

            return mergedPresets;
        });
    }

    @Override
    protected void apply(Map<String, Config.ShaderConfigPreset> data, ResourceManager p_10794_, ProfilerFiller p_10795_) {
        presets = data;
        if (Main.getConfig() != null) {
            Main.getConfig().loadDefaultPresets();
        }
    }
}
