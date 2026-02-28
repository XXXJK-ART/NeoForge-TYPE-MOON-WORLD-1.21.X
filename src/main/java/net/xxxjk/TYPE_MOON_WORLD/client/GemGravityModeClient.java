package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.GemGravityModeSelectScreen;

public final class GemGravityModeClient {
    private GemGravityModeClient() {
    }

    public static void openSelector(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        minecraft.setScreen(new GemGravityModeSelectScreen(hand));
    }
}
