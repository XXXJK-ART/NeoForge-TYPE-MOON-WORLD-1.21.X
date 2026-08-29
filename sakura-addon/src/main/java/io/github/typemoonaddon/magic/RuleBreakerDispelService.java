package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.data.ImaginarySpaceData.CursedArmorState;
import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.shadowlogic.magic.ShadowArtService;
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
import net.xxxjk.typemoonworld.api.TypeMoonWorldApi;

/** Public-API-only Rule Breaker integration for addon-owned state. */
public final class RuleBreakerDispelService {
    private static final ResourceLocation RULE_BREAKER = ResourceLocation.fromNamespaceAndPath(
        "typemoonworld",
        "rule_breaker"
    );
    private static final Set<ResourceLocation> GRAIL_MAGIC_IDS = Set.of(
        TypeMoonIntegration.SHADOW_MATERIALIZATION,
        TypeMoonIntegration.BLACK_MUD_CONTROL,
        TypeMoonIntegration.SUMMON_BLACK_MUD,
        TypeMoonIntegration.SHADOW_BINDING,
        TypeMoonIntegration.SHADOW_TRANSFER,
        TypeMoonIntegration.HEROIC_SPIRIT_DEVOURER,
        TypeMoonIntegration.SHADOW_ART
    );

    public static void capturePreGrailState(ServerPlayer player) {
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        if (data.grailPowerSnapshot() != null) {
            return;
        }
        var mana = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().mana(player);
        data.captureGrailPowerSnapshot(mana.current(), mana.maximum(), 0.0D, 0.0D, false, true);
    }

    public static boolean canDispel(LivingEntity target) {
        if (!(target instanceof ServerPlayer player) || !player.isAlive()) {
            return false;
        }
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
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
        var data = player.getData(ModAttachments.IMAGINARY_SPACE.get());
        boolean ascended = data.grailWormAscended();
        boolean protection = data.protectionActive();
        boolean cursedArmor = data.cursedArmorPresent();
        boolean shadowArt = data.shadowArtUnlocked();
        if (!ascended && !protection && !shadowArt && data.cursedArmorState() == CursedArmorState.DISSOLVING) {
            return false;
        }
        if (!ascended && !protection && !cursedArmor && !shadowArt) {
            return false;
        }

        if (cursedArmor) {
            CursedArmorService.beginDissolution(player);
        }
        if (shadowArt) {
            ShadowArtService.deactivate(player, true);
        }
        if (ascended) {
            boolean unlockSakuraEffects = data.grailErosionFull() && data.crestWormExpelled();
            BlackShadowNightService.playerUnavailable(player);
            ImaginaryShadowService.stopForUpgrade(player);
            ShadowMaterializationService.playerLoggedOut(player);
            ShadowTransferService.playerLoggedOut(player);
            SummonBlackMudService.playerDied(player);
            BlackMudControlService.dismissForDispel(player);
            HolyGrailService.dispel(player);
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
        CursedArmorService.sync(player);
        if (feedback) {
            player.displayClientMessage(
                Component.translatable(ascended
                    ? "message.typemoonaddon.rule_breaker.grail_removed"
                    : "message.typemoonaddon.rule_breaker.protection_removed"),
                true
            );
            player.serverLevel().sendParticles(
                ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                24, 0.6D, 0.8D, 0.6D, 0.08D
            );
            player.serverLevel().playSound(
                null, player.blockPosition(), SoundEvents.GLASS_BREAK,
                SoundSource.PLAYERS, 0.8F, 0.8F
            );
        }
        return true;
    }

    public static boolean lockGrailMagicKnowledge(ServerPlayer player) {
        // Public API has no unlearn operation. Addon executors enforce the lock.
        return player != null;
    }

    public static boolean unlockGrailMagicKnowledge(ServerPlayer player) {
        var knowledge = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().knowledge(player);
        boolean ready = true;
        for (ResourceLocation id : GRAIL_MAGIC_IDS) {
            ready &= knowledge.isLearned(id) || knowledge.learn(id);
        }
        return ready;
    }

    public static boolean unlockShadowArtKnowledge(ServerPlayer player) {
        return learn(player, TypeMoonIntegration.SHADOW_ART);
    }

    public static boolean unlockForbiddenMagicKnowledge(ServerPlayer player) {
        return learn(player, TypeMoonIntegration.FORBIDDEN_MAGIC);
    }

    private static boolean learn(ServerPlayer player, ResourceLocation id) {
        var knowledge = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().knowledge(player);
        return knowledge.isLearned(id) || knowledge.learn(id);
    }

    private static void restoreCurrentMana(
        ServerPlayer player,
        io.github.typemoonaddon.data.ImaginarySpaceData data
    ) {
        var snapshot = data.grailPowerSnapshot();
        if (snapshot == null) {
            return;
        }
        var mana = TypeMoonWorldApi.addon(TypeMoonAddon.MOD_ID).magics().mana(player);
        double desired = Math.clamp(snapshot.mana(), 0.0D, mana.maximum());
        if (mana.current() > desired) {
            mana.tryConsume(mana.current() - desired);
        } else {
            mana.add(desired - mana.current());
        }
        data.clearGrailPowerSnapshot();
    }

    private RuleBreakerDispelService() {
    }
}
