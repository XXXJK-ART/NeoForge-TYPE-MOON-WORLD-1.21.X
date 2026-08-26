package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.compat.TypeMoonServantBridge;
import io.github.typemoonaddon.config.GameplayConfig;
import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.registry.ModItems;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Advances the three-step Holy Grail erosion gate and grants its magic knowledge. */
public final class GrailErosionService {
    public static boolean record(ServerPlayer player) {
        return record(player, null, "unspecified");
    }

    public static boolean record(
        ServerPlayer player,
        @Nullable LivingEntity victim,
        String attribution
    ) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        int before = data.grailErosion();
        if (!data.grailWormAscended()) {
            logOutcome(player, victim, attribution, before, before, false, "grail_worm_not_ascended");
            return false;
        }
        data.markServantOutcome(dayIndex(player));
        long completionTick = player.server.overworld().getGameTime()
            + GameplayConfig.GRAIL_EROSION_FINAL_DELAY_TICKS;
        if (!data.increaseGrailErosion(completionTick)) {
            logOutcome(player, victim, attribution, before, data.grailErosion(), false, "erosion_already_full");
            return false;
        }

        boolean pending = data.grailErosionPending();
        if (pending) {
            BlackShadowNightService.playerUnavailable(player);
        }
        player.syncData(ModAttachments.IMAGINARY_SPACE.get());
        player.displayClientMessage(Component.translatable(
            pending
                ? "message.typemoonaddon.grail_erosion.pending"
                : "message.typemoonaddon.grail_erosion.progress",
            data.grailErosion(),
            3
        ), false);
        GrailParticleService.send(
            player.serverLevel(),
            player,
            ParticleTypes.SQUID_INK,
            player.getX(),
            player.getY() + 1.0D,
            player.getZ(),
            pending ? 36 : 16,
            0.45D,
            0.75D,
            0.45D,
            0.04D
        );
        player.serverLevel().playSound(
            null,
            player.blockPosition(),
            pending ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.SCULK_CATALYST_BLOOM,
            SoundSource.PLAYERS,
            pending ? 1.0F : 0.7F,
            pending ? 0.6F : 0.75F
        );
        logOutcome(player, victim, attribution, before, data.grailErosion(), true, "recorded");
        return true;
    }

    private static void logOutcome(
        ServerPlayer player,
        @Nullable LivingEntity victim,
        String attribution,
        int before,
        int after,
        boolean accepted,
        String reason
    ) {
        String victimType = victim == null
            ? "none"
            : BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();
        String servantId = victim == null ? null : TypeMoonServantBridge.servantId(victim);
        TypeMoonAddon.LOGGER.info(
            "Grail erosion outcome player={} victim_type={} servant_id={} attribution={} accepted={} reason={} erosion={}->{}",
            player.getGameProfile().getName(),
            victimType,
            servantId == null ? "unknown" : servantId,
            attribution,
            accepted,
            reason,
            before,
            after
        );
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
            if (!data.completePendingGrailErosion(now)) {
                continue;
            }
            data.unlockShadowArt();
            if (!TypeMoonIntegration.ensureShadowArtKnowledge(player)) {
                io.github.typemoonaddon.TypeMoonAddon.LOGGER.error(
                    "Could not grant Shadow Art knowledge to {} after Grail erosion completed",
                    player.getGameProfile().getName()
                );
            }
            if (!TypeMoonIntegration.ensureGrailWormPower(player)) {
                RuleBreakerDispelService.unlockGrailMagicKnowledge(player);
            }
            ensureCrestWormExpelled(player);
            CursedArmorService.beginFormation(player);
            BlackShadowNightService.playerUnavailable(player);
            player.syncData(ModAttachments.IMAGINARY_SPACE.get());
            player.displayClientMessage(Component.translatable(
                "message.typemoonaddon.grail_erosion.completed"
            ), false);
            GrailParticleService.send(
                player.serverLevel(),
                player,
                ParticleTypes.SQUID_INK,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                48,
                0.55D,
                0.9D,
                0.55D,
                0.06D
            );
            player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS,
                1.0F,
                0.6F
            );
        }
    }

    public static boolean hasOutcomeTonight(ServerPlayer player) {
        return player.getData(ModAttachments.IMAGINARY_SPACE.get()).hasServantOutcomeOn(dayIndex(player));
    }

    private static long dayIndex(ServerPlayer player) {
        return Math.floorDiv(player.serverLevel().getDayTime(), 24000L);
    }

    public static boolean ensureCrestWormExpelled(ServerPlayer player) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (!data.grailWormAscended() || !data.grailErosionFull() || !data.expelCrestWorm()) {
            return false;
        }
        ItemStack expelledWorm = new ItemStack(ModItems.CREST_WORM.get());
        if (!player.addItem(expelledWorm)) {
            player.drop(expelledWorm, false);
        }
        return true;
    }

    private GrailErosionService() {
    }
}
