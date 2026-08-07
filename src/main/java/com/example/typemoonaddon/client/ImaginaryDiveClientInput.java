package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.client.gui.ImaginaryDiveScreen;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceService;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceAttachments;
import com.example.typemoonaddon.imaginary_space.ImaginarySpaceData;
import com.example.typemoonaddon.magic.ImaginaryDiveMagic;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

/** Reuses the main mod's mode-switch key as the depth panel entry point. */
@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, value = Dist.CLIENT)
public final class ImaginaryDiveClientInput {
    private static boolean modeKeyDown;

    private ImaginaryDiveClientInput() {
    }

    @SubscribeEvent
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            modeKeyDown = false;
            return;
        }

        boolean rawDown = TypeMoonWorldModKeyMappings.MAGIC_MODE_SWITCH.isDown();
        if (!modeKeyDown && rawDown && minecraft.screen == null) {
            TypeMoonWorldModVariables.PlayerVariables vars =
                    player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
            boolean selected = PlayerMagicSelectionService.isCurrentSelection(
                    vars, ImaginaryDiveMagic.MAGIC_ID);
            if (selected && vars.is_magus && vars.is_magic_circuit_open
                    && player.isAlive() && !player.isSpectator()) {
                ImaginarySpaceData data = player.getData(ImaginarySpaceAttachments.PLAYER_STATE);
                if (ImaginarySpaceService.DIMENSION.equals(player.level().dimension()) && data.active()) {
                    minecraft.setScreen(new ImaginaryDiveScreen(data.depth()));
                } else {
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            "message.typemoonworld.imaginary_dive.needs_space"), true);
                }
            }
        }
        modeKeyDown = rawDown;
    }
}
