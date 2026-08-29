package io.github.typemoonaddon.client;

import io.github.typemoonaddon.network.EffectPayload;
import io.github.typemoonaddon.network.DeathFadePayload;
import io.github.typemoonaddon.network.VoidAbsorptionLinkPayload;
import io.github.typemoonaddon.shadowlogic.network.ShadowBindingEffectPayload;
import io.github.typemoonaddon.network.OpenShadowTransferPayload;
import io.github.typemoonaddon.network.OpenDevourerSelectionPayload;
import io.github.typemoonaddon.network.NightShadowCameraPayload;
import io.github.typemoonaddon.network.MagicOutputShockwavePayload;
import net.minecraft.client.Minecraft;

/** Loaded only when a clientbound payload is actually handled. */
public final class ClientPayloadBridge {
    public static void accept(EffectPayload payload) {
        ClientEffects.start(payload);
    }

    public static void accept(DeathFadePayload payload) {
        ClientEffects.startDeathFade(payload);
    }

    public static void accept(VoidAbsorptionLinkPayload payload) {
        ClientEffects.maintainVoidAbsorptionLink(payload);
    }

    public static void accept(ShadowBindingEffectPayload payload) {
        ClientEffects.maintainShadowBinding(payload);
    }

    public static void accept(MagicOutputShockwavePayload payload) {
        ClientEffects.startMagicOutputShockwave(payload);
    }

    public static void accept(OpenShadowTransferPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setScreen(new ShadowTransferScreen(payload));
        }
    }

    public static void accept(OpenDevourerSelectionPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setScreen(new HeroicSpiritDevourerScreen(payload));
        }
    }

    public static void accept(NightShadowCameraPayload payload) {
        ClientNightShadowCamera.accept(payload);
    }

    private ClientPayloadBridge() {
    }
}
