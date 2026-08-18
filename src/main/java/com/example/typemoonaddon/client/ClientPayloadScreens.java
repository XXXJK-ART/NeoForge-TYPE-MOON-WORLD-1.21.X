package com.example.typemoonaddon.client;

import com.example.typemoonaddon.network.OpenDevourerSelectionPayload;
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
}
