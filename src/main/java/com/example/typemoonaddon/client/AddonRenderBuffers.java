package com.example.typemoonaddon.client;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/**
 * Scoped buffer source for level-stage addon visuals that write several
 * RenderTypes in one pass. The global shared BufferSource can finish one
 * RenderType when another one is requested, invalidating old VertexConsumers.
 */
final class AddonRenderBuffers implements AutoCloseable {
    private static final int SHARED_BUFFER_SIZE = 64 * 1024;
    private static final int FIXED_BUFFER_SIZE = 256 * 1024;

    private final ByteBufferBuilder sharedBuffer = new ByteBufferBuilder(SHARED_BUFFER_SIZE);
    private final List<ByteBufferBuilder> fixedBuffers = new ArrayList<>();
    private final MultiBufferSource.BufferSource source;

    private AddonRenderBuffers(RenderType... fixedTypes) {
        LinkedHashMap<RenderType, ByteBufferBuilder> fixed = new LinkedHashMap<>();
        for (RenderType renderType : fixedTypes) {
            ByteBufferBuilder buffer = new ByteBufferBuilder(FIXED_BUFFER_SIZE);
            fixed.put(renderType, buffer);
            fixedBuffers.add(buffer);
        }
        source = MultiBufferSource.immediateWithBuffers(fixed, sharedBuffer);
    }

    static AddonRenderBuffers fixed(RenderType... fixedTypes) {
        return new AddonRenderBuffers(fixedTypes);
    }

    MultiBufferSource.BufferSource source() {
        return source;
    }

    @Override
    public void close() {
        source.endBatch();
        for (ByteBufferBuilder buffer : fixedBuffers) {
            buffer.close();
        }
        sharedBuffer.close();
    }
}
