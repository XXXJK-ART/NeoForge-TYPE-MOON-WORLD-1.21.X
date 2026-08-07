package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.ImaginarySpaceMagic;
import com.example.typemoonaddon.network.ImaginarySpaceModeSwitchPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.xxxjk.TYPE_MOON_WORLD.init.TypeMoonWorldModKeyMappings;
import net.xxxjk.TYPE_MOON_WORLD.magic.PlayerMagicSelectionService;
import net.xxxjk.TYPE_MOON_WORLD.network.TypeMoonWorldModVariables;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID, value = Dist.CLIENT)
public final class ImaginarySpaceClientInput {
    private static boolean modeKeyDown;
    private static boolean modeOwned;

    private ImaginarySpaceClientInput() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            modeKeyDown = false;
            modeOwned = false;
            return;
        }
        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        boolean selected = PlayerMagicSelectionService.isCurrentSelection(
                vars, ImaginarySpaceMagic.MAGIC_ID);
        boolean canSend = minecraft.screen == null
                && selected
                && vars.is_magus
                && vars.is_magic_circuit_open
                && player.isAlive()
                && !player.isSpectator();

        boolean rawModeDown = TypeMoonWorldModKeyMappings.MAGIC_MODE_SWITCH.isDown();
        if (!modeKeyDown && rawModeDown) {
            modeOwned = canSend;
            if (modeOwned) {
                PacketDistributor.sendToServer(new ImaginarySpaceModeSwitchPayload());
            }
        } else if (modeKeyDown && !rawModeDown) {
            modeOwned = false;
        }
        modeKeyDown = rawModeDown;
    }
}
