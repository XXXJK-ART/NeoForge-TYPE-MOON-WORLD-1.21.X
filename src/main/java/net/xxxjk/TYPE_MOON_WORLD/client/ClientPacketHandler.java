package net.xxxjk.TYPE_MOON_WORLD.client;

import net.minecraft.client.Minecraft;
import net.xxxjk.TYPE_MOON_WORLD.client.gui.ProjectionPresetScreen;
import net.minecraft.world.entity.player.Player;

public class ClientPacketHandler {
    public static void openProjectionGui() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            Minecraft.getInstance().setScreen(new ProjectionPresetScreen(player));
        }
    }
}
