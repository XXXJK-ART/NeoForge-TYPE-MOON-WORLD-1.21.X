package com.example.typemoonaddon.event;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.zagan.ZaganService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = TypeMoonAddon.MOD_ID)
public final class ZaganEvents {
    private ZaganEvents() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ZaganService.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        ZaganService.handleEntityInvalidated(event.getEntity());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ZaganService.stop(event.getEntity(), false);
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        ZaganService.stop(event.getEntity(), false);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        ZaganService.stop(event.getOriginal(), false);
        ZaganService.stop(event.getEntity(), false);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        ZaganService.stop(event.getEntity(), false);
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ZaganService.handleChunkUnload(level, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ZaganService.clearAll(event.getServer());
    }
}
