package io.github.typemoonaddon.client;

import io.github.typemoonaddon.network.NightShadowCameraPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Maintains the night-mission camera without moving the server-side player out of bed. */
public final class ClientNightShadowCamera {
    private static int requestedEntityId = -1;
    private static int controlledEntityId = -1;

    public static void accept(NightShadowCameraPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (payload.active()) {
            requestedEntityId = payload.shadowEntityId();
            tick(minecraft);
            return;
        }
        if (payload.shadowEntityId() != requestedEntityId
            && payload.shadowEntityId() != controlledEntityId) {
            return;
        }
        requestedEntityId = -1;
        restorePlayerCamera(minecraft);
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            requestedEntityId = -1;
            controlledEntityId = -1;
            return;
        }
        if (requestedEntityId < 0) {
            return;
        }

        Entity shadow = minecraft.level.getEntity(requestedEntityId);
        if (shadow == null || shadow.isRemoved()) {
            if (controlledEntityId == requestedEntityId) {
                restorePlayerCamera(minecraft);
            }
            return;
        }
        controlledEntityId = requestedEntityId;
        if (minecraft.getCameraEntity() != shadow) {
            minecraft.setCameraEntity(shadow);
        }
    }

    private static void restorePlayerCamera(Minecraft minecraft) {
        Entity camera = minecraft.getCameraEntity();
        if (minecraft.player != null
            && (controlledEntityId < 0 || (camera != null && camera.getId() == controlledEntityId))) {
            minecraft.setCameraEntity(minecraft.player);
        }
        controlledEntityId = -1;
    }

    private ClientNightShadowCamera() {
    }
}
