package com.qendolin.betterclouds.compat;


public class IrisCompat {
    public static final boolean IS_LOADED = false;// FabricLoader.getInstance().isModLoaded("iris");

    public static boolean isShadersEnabled() {
        return IS_LOADED;// && Iris.getIrisConfig().areShadersEnabled() && Iris.getCurrentPack().isPresent();
    }

    public static void bindFramebuffer() {
       /* WorldRenderingPipeline pipeline = Iris.getPipelineManager().getPipelineNullable();
        if (pipeline instanceof NewWorldRenderingPipeline corePipeline) {
            ExtendedShaderAccessor irisShader = (ExtendedShaderAccessor) corePipeline.getShaderMap().getShader(ShaderKey.CLOUDS);
            if (corePipeline.isBeforeTranslucent) {
                irisShader.getWritingToBeforeTranslucent().bindAsDrawBuffer();
            } else {
                irisShader.getWritingToAfterTranslucent().bindAsDrawBuffer();
            }
        }*/
    }
}
