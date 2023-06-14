package com.qendolin.betterclouds.mixin;

import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferUploader.class)
public interface BufferRendererAccessor {
    @Accessor("lastImmediateBuffer")
    static VertexBuffer getCurrentVertexBuffer() {
        throw new AssertionError();
    }

    @Accessor("lastImmediateBuffer")
    static void setCurrentVertexBuffer(VertexBuffer buffer) {
        throw new AssertionError();
    }

}
