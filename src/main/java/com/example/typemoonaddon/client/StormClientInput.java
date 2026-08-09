package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.StormMagic;
import com.example.typemoonaddon.network.StormCastInputPayload;
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
public final class StormClientInput {
    private static boolean castKeyDown;
    private static boolean stormCastOwned;

    private StormClientInput() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            castKeyDown = false;
            stormCastOwned = false;
            return;
        }

        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        boolean selected = PlayerMagicSelectionService.isCurrentSelection(vars, StormMagic.MAGIC_ID);
        boolean canChannel = minecraft.screen == null
                && selected
                && vars.is_magus
                && vars.is_magic_circuit_open
                && vars.magic_cooldown <= 0.0D
                && player.isAlive()
                && !player.isSpectator();

        boolean rawCastDown = TypeMoonWorldModKeyMappings.CAST_MAGIC.isDown();
        if (!castKeyDown && rawCastDown) {
            stormCastOwned = canChannel;
            if (stormCastOwned) {
                PacketDistributor.sendToServer(new StormCastInputPayload(StormCastInputPayload.PRESS));
            }
        } else if (castKeyDown && !rawCastDown) {
            if (stormCastOwned) {
                PacketDistributor.sendToServer(new StormCastInputPayload(StormCastInputPayload.RELEASE));
            }
            stormCastOwned = false;
        } else if (rawCastDown && stormCastOwned && !canChannel) {
            PacketDistributor.sendToServer(new StormCastInputPayload(StormCastInputPayload.CANCEL));
            stormCastOwned = false;
        }
        castKeyDown = rawCastDown;
    }
}
