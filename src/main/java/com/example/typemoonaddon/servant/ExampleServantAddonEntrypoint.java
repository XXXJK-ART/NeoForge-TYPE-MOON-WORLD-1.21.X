package com.example.typemoonaddon.servant;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonEntrypoint;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.IServantAddonRegistry;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantCombatActionContext;
import net.xxxjk.TYPE_MOON_WORLD.servant.api.ServantExecutionResult;

public final class ExampleServantAddonEntrypoint implements IServantAddonEntrypoint {
    public static final String ACTION_ID = "addon_arcane_burst";
    private static final String LAST_CAST_TICK = "TypeMoonAddonLastArcaneBurst";
    private static final int COOLDOWN_TICKS = 80;

    @Override
    public String providerId() {
        return "typemoonworld";
    }

    @Override
    public void registerServants(IServantAddonRegistry registry) {
        registry.registerCombatAction(ACTION_ID, this::castArcaneBurst, providerId());
    }

    private ServantExecutionResult castArcaneBurst(ServantCombatActionContext context) {
        LivingEntity caster = context.caster();
        LivingEntity target = context.target();
        if (caster == null || target == null || !target.isAlive()
                || context.distance() > 7.0 || !context.hasLineOfSight()) {
            return ServantExecutionResult.NOT_HANDLED;
        }

        CompoundTag data = caster.getPersistentData();
        long lastCastTick = data.getLong(LAST_CAST_TICK);
        if (lastCastTick > 0 && context.gameTick() - lastCastTick < COOLDOWN_TICKS) {
            return ServantExecutionResult.NOT_HANDLED;
        }

        data.putLong(LAST_CAST_TICK, context.gameTick());
        float damage = (float) Math.max(4.0, caster.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8);
        target.invulnerableTime = 0;
        target.hurt(caster.damageSources().magic(), damage);

        if (caster.level() instanceof ServerLevel level) {
            level.sendParticles(
                    ParticleTypes.ENCHANT,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5,
                    target.getZ(),
                    28,
                    0.6,
                    0.8,
                    0.6,
                    0.08
            );
            level.playSound(
                    null,
                    target.blockPosition(),
                    SoundEvents.EVOKER_CAST_SPELL,
                    SoundSource.HOSTILE,
                    1.0F,
                    1.25F
            );
        }
        return ServantExecutionResult.SUCCESS;
    }
}
