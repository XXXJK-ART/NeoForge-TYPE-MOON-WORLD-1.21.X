package net.xxxjk.TYPE_MOON_WORLD.chain.client;

import net.xxxjk.TYPE_MOON_WORLD.chain.compat.TypeMoonBridge;
import net.xxxjk.TYPE_MOON_WORLD.chain.network.ChainInputPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = "typemoonworld", value = {Dist.CLIENT})
public final class ClientInputEvents {
    private static boolean previousUseState;

    public static void onUsePressed() {
        updateUseState(true);
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null || minecraft.screen != null
            || !minecraft.options.keyUse.matchesMouse(event.getButton())) {
            return;
        }
        if (event.getAction() == GLFW.GLFW_PRESS) {
            updateUseState(true);
        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            updateUseState(false);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            previousUseState = false;
            return;
        }
        if (minecraft.screen != null) {
            if (previousUseState) {
                PacketDistributor.sendToServer(new ChainInputPayload(false));
                previousUseState = false;
            }
            return;
        }
        boolean useDown = minecraft.options.keyUse.isDown();
        updateUseState(useDown);
    }

    private static void updateUseState(boolean useDown) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean eligible = minecraft.player != null && minecraft.getConnection() != null
            && TypeMoonBridge.isSyncedEnkiduPlayer(minecraft.player);
        if (!eligible) {
            if (previousUseState && minecraft.getConnection() != null) {
                PacketDistributor.sendToServer(new ChainInputPayload(false));
            }
            previousUseState = false;
            return;
        }
        if (useDown == previousUseState) {
            return;
        }
        PacketDistributor.sendToServer(new ChainInputPayload(useDown));
        previousUseState = useDown;
    }

    private ClientInputEvents() {
    }
}

