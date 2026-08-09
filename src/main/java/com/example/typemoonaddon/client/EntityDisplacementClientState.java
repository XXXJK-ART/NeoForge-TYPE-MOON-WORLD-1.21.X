package com.example.typemoonaddon.client;

import com.example.typemoonaddon.network.EntityDisplacementTargetPayload;

public final class EntityDisplacementClientState {
    private static int entityId = -1;
    private static float width;
    private static float height;
    private static float depth;

    private EntityDisplacementClientState() {
    }

    public static void apply(EntityDisplacementTargetPayload payload) {
        entityId = payload.entityId();
        width = Math.max(0.01F, payload.width());
        height = Math.max(0.01F, payload.height());
        depth = Math.max(0.01F, payload.depth());
    }

    public static int entityId() { return entityId; }
    public static float width() { return width; }
    public static float height() { return height; }
    public static float depth() { return depth; }
    public static void clear() { entityId = -1; }
}
