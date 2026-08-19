package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.config.GameplayConfig;
import com.example.typemoonaddon.registry.AddonAttachments;
import com.example.typemoonaddon.registry.AddonItems;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.xxxjk.TYPE_MOON_WORLD.servant.combat.ServantIdentityHelper;

/** Advances Sakura's three-step Holy Grail erosion gate. */
public final class SakuraGrailErosionService {
    public static boolean record(ServerPlayer player) {
        return record(player, null, "unspecified");
    }

    public static boolean record(ServerPlayer player, @Nullable LivingEntity victim, String attribution) {
        if (player == null) {
            return false;
        }
        var data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        int before = data.grailErosion();
        if (!data.grailWormAscended()) {
            logOutcome(player, victim, attribution, before, before, false, "grail_worm_not_ascended");
            return false;
        }
        data.markServantOutcome(dayIndex(player));
        long completionTick = player.server.overworld().getGameTime() + GameplayConfig.GRAIL_EROSION_FINAL_DELAY_TICKS;
        if (!data.increaseGrailErosion(completionTick)) {
            logOutcome(player, victim, attribution, before, data.grailErosion(), false, "erosion_already_full");
            return false;
        }

        boolean pending = data.grailErosionPending();
        if (pending) {
            BlackShadowNightService.playerUnavailable(player);
            SakuraShadowMaterializationService.playerUnavailable(player);
        }
        AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
        player.displayClientMessage(Component.translatable(
                pending
                        ? "message.typemoonworld.grail_erosion.pending"
                        : "message.typemoonworld.grail_erosion.progress",
                data.grailErosion(),
                3
        ), false);
        SakuraParticleService.send(
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

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            var data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
            if (!data.completePendingGrailErosion(now)) {
                continue;
            }
            data.unlockShadowArt();
            if (!SakuraTypeMoonIntegration.ensureShadowArtKnowledge(player)) {
                TypeMoonAddon.LOGGER.error(
                        "Could not grant Shadow Art knowledge to {} after Grail erosion completed",
                        player.getGameProfile().getName()
                );
            }
            if (!SakuraTypeMoonIntegration.ensureGrailWormPower(player)) {
                SakuraTypeMoonIntegration.ensureForbiddenMagicKnowledge(player);
            }
            ensureCrestWormExpelled(player);
            CursedArmorService.beginFormation(player);
            BlackShadowNightService.playerUnavailable(player);
            SakuraShadowMaterializationService.playerUnavailable(player);
            AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
            player.displayClientMessage(Component.translatable("message.typemoonworld.grail_erosion.completed"), false);
            SakuraParticleService.send(
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
        return player.getData(AddonAttachments.IMAGINARY_SPACE.get()).hasServantOutcomeOn(dayIndex(player));
    }

    public static boolean ensureCrestWormExpelled(ServerPlayer player) {
        var data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (!data.grailWormAscended() || !data.grailErosionFull() || !data.expelCrestWorm()) {
            return false;
        }
        ItemStack expelledWorm = new ItemStack(AddonItems.CREST_WORM.get());
        if (!player.addItem(expelledWorm)) {
            player.drop(expelledWorm, false);
        }
        AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
        return true;
    }

    private static long dayIndex(ServerPlayer player) {
        return Math.floorDiv(player.serverLevel().getDayTime(), 24000L);
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
        String servantId = victim == null || ServantIdentityHelper.definitionOf(victim) == null
                ? "unknown"
                : ServantIdentityHelper.definitionOf(victim).id().toString();
        TypeMoonAddon.LOGGER.info(
                "Grail erosion outcome player={} victim_type={} servant_id={} attribution={} accepted={} reason={} erosion={}->{}",
                player.getGameProfile().getName(),
                victimType,
                servantId,
                attribution,
                accepted,
                reason,
                before,
                after
        );
    }

    private SakuraGrailErosionService() {
    }
}
