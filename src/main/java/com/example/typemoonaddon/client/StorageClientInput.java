package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.StorageMagic;
import com.example.typemoonaddon.network.StorageCastInputPayload;
import com.example.typemoonaddon.network.StorageModeSwitchPayload;
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
public final class StorageClientInput {
    private static boolean castKeyDown;
    private static boolean modeKeyDown;
    private static boolean storageCastOwned;
    private static boolean storageModeOwned;

    private StorageClientInput() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            castKeyDown = false;
            storageCastOwned = false;
            modeKeyDown = false;
            storageModeOwned = false;
            return;
        }

        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        boolean selected = PlayerMagicSelectionService.isCurrentSelection(vars, StorageMagic.MAGIC_ID);
        boolean canSend = minecraft.screen == null
                && selected
                && vars.is_magus
                && vars.is_magic_circuit_open
                && player.isAlive()
                && !player.isSpectator();

        boolean rawCastDown = TypeMoonWorldModKeyMappings.CAST_MAGIC.isDown();
        if (!castKeyDown && rawCastDown) {
            storageCastOwned = canSend;
            if (storageCastOwned) {
                PacketDistributor.sendToServer(new StorageCastInputPayload(true));
            }
        } else if (castKeyDown && !rawCastDown) {
            releaseCastIfNeeded();
        }
        castKeyDown = rawCastDown;

        boolean rawModeDown = TypeMoonWorldModKeyMappings.MAGIC_MODE_SWITCH.isDown();
        if (!modeKeyDown && rawModeDown) {
            storageModeOwned = canSend;
            if (storageModeOwned) {
                PacketDistributor.sendToServer(new StorageModeSwitchPayload());
            }
        } else if (modeKeyDown && !rawModeDown) {
            storageModeOwned = false;
        }
        modeKeyDown = rawModeDown;
    }

    private static void releaseCastIfNeeded() {
        if (storageCastOwned) {
            storageCastOwned = false;
            PacketDistributor.sendToServer(new StorageCastInputPayload(false));
        }
    }
}
