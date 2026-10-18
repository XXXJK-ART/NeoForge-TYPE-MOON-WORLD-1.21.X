package com.captain.servant.network;

import com.captain.servant.network.payload.SpiritualizationStatePayload;
import com.captain.servant.network.payload.SpiritualizationTogglePayload;
import com.captain.servant.network.payload.TelepathyTogglePayload;
import com.captain.servant.network.payload.KnightArmoryOpenPayload;
import com.captain.servant.network.payload.KnightArmorySelectPayload;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import java.util.function.IntConsumer;

public final class FateSupportNetwork {
    private static IntConsumer armoryScreen = mask -> { };

    public static void setArmoryScreen(IntConsumer opener) {
        armoryScreen = opener;
    }

    public static void openArmoryScreen(int unlockedMask) {
        armoryScreen.accept(unlockedMask);
    }

    private FateSupportNetwork() {
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(
                SpiritualizationTogglePayload.TYPE,
                SpiritualizationTogglePayload.STREAM_CODEC,
                SpiritualizationTogglePayload::handleData
        );
        registrar.playToClient(
                SpiritualizationStatePayload.TYPE,
                SpiritualizationStatePayload.STREAM_CODEC,
                SpiritualizationStatePayload::handleData
        );
        registrar.playToServer(
                TelepathyTogglePayload.TYPE,
                TelepathyTogglePayload.STREAM_CODEC,
                TelepathyTogglePayload::handleData
        );
        registrar.playToClient(
                KnightArmoryOpenPayload.TYPE,
                KnightArmoryOpenPayload.STREAM_CODEC,
                KnightArmoryOpenPayload::handleData
        );
        registrar.playToServer(
                KnightArmorySelectPayload.TYPE,
                KnightArmorySelectPayload.STREAM_CODEC,
                KnightArmorySelectPayload::handleData
        );
    }
}
