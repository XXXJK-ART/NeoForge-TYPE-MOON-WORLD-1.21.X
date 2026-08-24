package com.example.typemoonaddon.magic;

import com.example.typemoonaddon.TypeMoonAddon;
import com.example.typemoonaddon.data.ImaginarySpaceData;
import com.example.typemoonaddon.registry.AddonAttachments;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class SakuraRuleBreakerDispelService {
    private static final ResourceLocation RULE_BREAKER = ResourceLocation.fromNamespaceAndPath("typemoonworld", "rule_breaker");
    private static final Set<ResourceLocation> GRAIL_MAGIC_IDS = Set.of(
            SakuraTypeMoonIntegration.SHADOW_MATERIALIZATION,
            SakuraTypeMoonIntegration.BLACK_MUD_CONTROL,
            SakuraTypeMoonIntegration.SUMMON_BLACK_MUD,
            SakuraTypeMoonIntegration.SHADOW_BINDING,
            SakuraTypeMoonIntegration.SHADOW_TRANSFER,
            SakuraTypeMoonIntegration.HEROIC_SPIRIT_DEVOURER,
            SakuraTypeMoonIntegration.SHADOW_ART
    );

    public static void capturePreGrailState(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ImaginarySpaceData data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        if (data.grailPowerSnapshot() != null) {
            return;
        }
        var mana = SakuraTypeMoonIntegration.registry().mana(player);
        data.captureGrailPowerSnapshot(mana.current(), mana.maximum(), 0.0D, 0.0D, false, true);
    }

    public static boolean canDispel(LivingEntity target) {
        if (!(target instanceof ServerPlayer player) || !player.isAlive()) {
            return false;
        }
        var data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        return data.protectionActive()
                || data.grailWormAscended()
                || data.cursedArmorPresent()
                || data.shadowArtUnlocked();
    }

    public static boolean dispelFromDamage(LivingEntity target, DamageSource source) {
        return isRuleBreakerDamage(source) && dispel(target);
    }

    public static boolean isRuleBreakerDamage(DamageSource source) {
        if (source == null) {
            return false;
        }
        ItemStack weapon = source.getWeaponItem();
        return weapon != null
                && !weapon.isEmpty()
                && RULE_BREAKER.equals(BuiltInRegistries.ITEM.getKey(weapon.getItem()));
    }

    public static boolean dispel(LivingEntity target) {
        return dispel(target, true);
    }

    static boolean dispelSilently(LivingEntity target) {
        return dispel(target, false);
    }

    private static boolean dispel(LivingEntity target, boolean feedback) {
        if (!(target instanceof ServerPlayer player)) {
            return false;
        }
        var data = player.getData(AddonAttachments.IMAGINARY_SPACE.get());
        boolean ascended = data.grailWormAscended();
        boolean protection = data.protectionActive();
        boolean cursedArmor = data.cursedArmorPresent();
        boolean shadowArt = data.shadowArtUnlocked();
        if (!ascended && !protection && !cursedArmor && !shadowArt) {
            return false;
        }

        if (data.cursedArmorPresent()) {
            CursedArmorService.beginDissolution(player);
        }
        if (data.shadowArtUnlocked()) {
            SakuraShadowArtService.deactivate(player, true);
        }
        if (ascended) {
            boolean unlockSakuraEffects = data.grailErosionFull() && data.crestWormExpelled();
            BlackShadowNightService.playerUnavailable(player);
            SakuraImaginaryShadowService.stopForUpgrade(player);
            SakuraShadowMaterializationService.playerUnavailable(player);
            SakuraShadowTransferService.playerUnavailable(player);
            SakuraSummonBlackMudService.playerUnavailable(player);
            SakuraBlackMudControlService.dismissForDispel(player);
            restoreCurrentMana(player, data);
            if (unlockSakuraEffects) {
                data.enableSakuraImaginaryEffects();
            }
            data.revokeGrailWormAscension();
            data.unlockForbiddenMagic();
            unlockForbiddenMagicKnowledge(player);
        } else {
            data.clearProtection();
        }
        AddonAttachments.sync(player, AddonAttachments.IMAGINARY_SPACE);
        CursedArmorService.sync(player);
        if (feedback) {
            player.displayClientMessage(
                    Component.translatable(ascended
                            ? "message.typemoonworld.rule_breaker.grail_removed"
                            : "message.typemoonworld.rule_breaker.protection_removed"),
                    true
            );
            player.serverLevel().sendParticles(
                    ParticleTypes.ENCHANT,
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ(),
                    24,
                    0.6D,
                    0.8D,
                    0.6D,
                    0.08D
            );
            player.serverLevel().playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS,
                    0.8F,
                    0.8F
            );
        }
        return true;
    }

    public static boolean lockGrailMagicKnowledge(ServerPlayer player) {
        return player != null;
    }

    public static boolean unlockGrailMagicKnowledge(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        var knowledge = SakuraTypeMoonIntegration.registry().knowledge(player);
        boolean ready = true;
        for (ResourceLocation id : GRAIL_MAGIC_IDS) {
            ready &= knowledge.isLearned(id) || knowledge.learn(id);
        }
        return ready;
    }

    public static boolean unlockShadowArtKnowledge(ServerPlayer player) {
        return learn(player, SakuraTypeMoonIntegration.SHADOW_ART);
    }

    public static boolean unlockForbiddenMagicKnowledge(ServerPlayer player) {
        return learn(player, SakuraTypeMoonIntegration.FORBIDDEN_MAGIC);
    }

    private static boolean learn(ServerPlayer player, ResourceLocation id) {
        if (player == null) {
            return false;
        }
        var knowledge = SakuraTypeMoonIntegration.registry().knowledge(player);
        return knowledge.isLearned(id) || knowledge.learn(id);
    }

    private static void restoreCurrentMana(ServerPlayer player, ImaginarySpaceData data) {
        ImaginarySpaceData.GrailPowerSnapshot snapshot = data.grailPowerSnapshot();
        if (snapshot == null) {
            return;
        }
        var mana = SakuraTypeMoonIntegration.registry().mana(player);
        double desired = Math.clamp(snapshot.mana(), 0.0D, mana.maximum());
        if (mana.current() > desired) {
            mana.tryConsume(mana.current() - desired);
        } else {
            mana.add(desired - mana.current());
        }
        data.clearGrailPowerSnapshot();
    }

    private SakuraRuleBreakerDispelService() {
    }
}
