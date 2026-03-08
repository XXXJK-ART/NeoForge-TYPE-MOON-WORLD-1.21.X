package net.xxxjk.TYPE_MOON_WORLD.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.xxxjk.TYPE_MOON_WORLD.TYPE_MOON_WORLD;
import net.xxxjk.TYPE_MOON_WORLD.entity.GanderProjectileEntity;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGander;
import net.xxxjk.TYPE_MOON_WORLD.magic.other.MagicGandrMachineGun;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = TYPE_MOON_WORLD.MOD_ID)
public class GanderLifecycleCleanupEvents {
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        cleanupPlayerState(player);
        if (player.getServer() == null) {
            return;
        }
        for (ServerLevel level : player.getServer().getAllLevels()) {
            discardOwnedGanders(level, player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        cleanupPlayerState(player);
        if (player.level() instanceof ServerLevel currentLevel) {
            discardOwnedGanders(currentLevel, player);
        }
        if (player.getServer() == null) {
            return;
        }
        ServerLevel fromLevel = player.getServer().getLevel(event.getFrom());
        if (fromLevel != null && fromLevel != player.level()) {
            discardOwnedGanders(fromLevel, player);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        cleanupPlayerState(player);
        if (player.level() instanceof ServerLevel level) {
            discardOwnedGanders(level, player);
        }
    }

    private static void cleanupPlayerState(ServerPlayer player) {
        MagicGander.forceCleanup(player);
        MagicGandrMachineGun.forceCleanup(player);
    }

    private static void discardOwnedGanders(ServerLevel level, ServerPlayer owner) {
        List<GanderProjectileEntity> pending = new ArrayList<>();
        for (Entity entity : level.getEntities().getAll()) {
            if (entity instanceof GanderProjectileEntity projectile && projectile.isOwnedBy(owner)) {
                pending.add(projectile);
            }
        }
        for (GanderProjectileEntity projectile : pending) {
            projectile.discard();
        }
    }
}
