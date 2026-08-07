package com.example.typemoonaddon.client;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.magic.ZaganMagic;
import com.example.typemoonaddon.network.ZaganCastInputPayload;
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
public final class ZaganClientInput {
    private static boolean castKeyDown;

    private ZaganClientInput() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            castKeyDown = false;
            return;
        }

        TypeMoonWorldModVariables.PlayerVariables vars =
                player.getData(TypeMoonWorldModVariables.PLAYER_VARIABLES);
        boolean selected = PlayerMagicSelectionService.isCurrentSelection(vars, ZaganMagic.MAGIC_ID);
        boolean canCast = minecraft.screen == null
                && selected
                && vars.is_magus
                && vars.is_magic_circuit_open
                && vars.magic_cooldown <= 0.0D
                && player.isAlive()
                && !player.isSpectator();
        boolean rawCastDown = TypeMoonWorldModKeyMappings.CAST_MAGIC.isDown();
        if (!castKeyDown && rawCastDown && canCast) {
            PacketDistributor.sendToServer(new ZaganCastInputPayload(ZaganCastInputPayload.PRESS));
        } else if (castKeyDown && !rawCastDown) {
            PacketDistributor.sendToServer(new ZaganCastInputPayload(ZaganCastInputPayload.CANCEL));
        } else if (rawCastDown && !canCast) {
            PacketDistributor.sendToServer(new ZaganCastInputPayload(ZaganCastInputPayload.CANCEL));
        }
        castKeyDown = rawCastDown;
    }
}
