package com.example.typemoonaddon.client;

import com.example.typemoonaddon.network.OpenDevourerSelectionPayload;
import com.example.typemoonaddon.network.OpenBoundaryImmunityPayload;
import com.example.typemoonaddon.network.OpenDetectionWormControlPayload;
import com.example.typemoonaddon.network.OpenShadowTransferPayload;
import net.minecraft.client.Minecraft;

public final class ClientPayloadScreens {
    private ClientPayloadScreens() {
    }

    public static void openDevourerSelection(OpenDevourerSelectionPayload payload) {
        Minecraft.getInstance().setScreen(new HeroicSpiritDevourerScreen(payload));
    }

    public static void openShadowTransfer(OpenShadowTransferPayload payload) {
        Minecraft.getInstance().setScreen(new ShadowTransferScreen(payload));
    }

    public static void openBoundaryImmunity(OpenBoundaryImmunityPayload payload) {
        Minecraft.getInstance().setScreen(new BoundaryImmunityScreen(payload));
    }

    public static void openDetectionWormControl(OpenDetectionWormControlPayload payload) {
        Minecraft.getInstance().setScreen(new DetectionWormControlScreen(payload));
    }
}
