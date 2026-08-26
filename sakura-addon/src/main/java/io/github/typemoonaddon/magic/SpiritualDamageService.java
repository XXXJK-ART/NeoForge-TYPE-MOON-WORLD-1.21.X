package io.github.typemoonaddon.magic;

import io.github.typemoonaddon.TypeMoonAddon;
import io.github.typemoonaddon.compat.TypeMoonServantBridge;
import io.github.typemoonaddon.config.GameplayConfig;
import io.github.typemoonaddon.registry.ModAttachments;
import io.github.typemoonaddon.registry.ModEffects;
import io.github.typemoonaddon.shadowlogic.magic.ShadowArtService;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public final class SpiritualDamageService {
    public static final ResourceKey<net.minecraft.world.damagesource.DamageType> COLLAPSE = ResourceKey.create(Registries.DAMAGE_TYPE, TypeMoonAddon.id("spiritual_collapse"));
    public static boolean appliesTo(LivingEntity entity) { return TypeMoonServantBridge.isActualServant(entity); }
    public static boolean add(LivingEntity target, float percent) {
        if (!appliesTo(target) || target.level().isClientSide()) return false;
        var data = target.getData(ModAttachments.SPIRITUAL_DAMAGE.get());
        boolean changed = data.add(percent, target.level().getGameTime()); if (changed) sync(target); return changed;
    }
    public static boolean raiseTo(LivingEntity target, float percent) {
        if (!appliesTo(target) || target.level().isClientSide()) return false;
        var data = target.getData(ModAttachments.SPIRITUAL_DAMAGE.get());
        float targetPercent = Math.clamp(percent, 0.0F, 100.0F);
        boolean changed = data.add(targetPercent - data.percent(), target.level().getGameTime());
        if (changed) sync(target);
        return changed;
    }
    public static void recordMagicCost(LivingEntity target, double cost) {
        if (!ShadowArtService.isPierced(target) || cost <= 0 || !appliesTo(target)) return;
        var data = target.getData(ModAttachments.SPIRITUAL_DAMAGE.get());
        if (data.recordMana(cost, target.level().getGameTime())) sync(target);
    }
    public static void tick(LivingEntity target) {
        if (target.level().isClientSide()) return;
        if (!appliesTo(target)) {
            var invalidData = target.getExistingDataOrNull(ModAttachments.SPIRITUAL_DAMAGE.get());
            if (invalidData != null && invalidData.percent() > 0.0F) {
                invalidData.clearAfterCollapse();
                target.syncData(ModAttachments.SPIRITUAL_DAMAGE.get());
            }
            target.removeEffect(ModEffects.SPIRITUAL_DAMAGE);
            return;
        }
        if (target.tickCount % 20 == 0 && ShadowArtService.isPierced(target)) {
            add(target, GameplayConfig.SPIRIT_ORIGIN_DAMAGE_PER_SECOND);
        }
        var data = target.getExistingDataOrNull(ModAttachments.SPIRITUAL_DAMAGE.get());
        if (data == null || data.percent() <= 0) return;
        target.forceAddEffect(new MobEffectInstance(ModEffects.SPIRITUAL_DAMAGE, 40, Math.clamp((int)data.percent() / 10, 0, 9), false, true, true), null);
        long deadline = data.deathDeadline(); if (deadline > 0 && target.level().getGameTime() >= deadline && !data.collapseExecuting()) collapse(target, data);
    }
    public static boolean isCollapse(DamageSource source) { return source != null && source.is(COLLAPSE); }
    private static void collapse(LivingEntity target, io.github.typemoonaddon.data.SpiritualDamageData data) {
        data.setCollapseExecuting(true);
        DamageSource source = new DamageSource(((ServerLevel)target.level()).registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(COLLAPSE));
        TypeMoonAddon.LOGGER.info("Spiritual origin collapse executing for {} at {}%", target.getScoreboardName(), data.percent());
        target.hurt(source, Float.MAX_VALUE);
        if (target.isAlive()) {
            target.setHealth(0);
            target.die(source);
        }
        if (target.isAlive()) data.setCollapseExecuting(false);
    }
    public static void completeCollapseDeath(LivingEntity target) {
        var data = target.getExistingDataOrNull(ModAttachments.SPIRITUAL_DAMAGE.get());
        if (data == null) return;
        data.clearAfterCollapse();
        target.syncData(ModAttachments.SPIRITUAL_DAMAGE.get());
        target.removeEffect(ModEffects.SPIRITUAL_DAMAGE);
    }
    private static void sync(LivingEntity target) {
        target.syncData(ModAttachments.SPIRITUAL_DAMAGE.get());
        if (target instanceof net.minecraft.server.level.ServerPlayer player) {
            player.displayClientMessage(Component.translatable("message.typemoonaddon.spiritual_damage", Math.round(target.getData(ModAttachments.SPIRITUAL_DAMAGE.get()).percent())), true);
        }
    }
    private SpiritualDamageService() {}
}
