package net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

public class UBWBrokenPhantasmExplosion {

    public static void explode(Level level, Entity source, Entity owner, ItemStack stack, Vec3 pos) {
        if (level.isClientSide) return;

        double cost = MagicBrokenPhantasm.calculateCost(stack, false);
        float explosionPower = (float)(cost / 20.0);
        if (explosionPower < 2.0f) explosionPower = 2.0f;

        float damagePower = explosionPower;
        float radiusPower = Math.min(explosionPower, 6.0f);
        if (radiusPower < 3.0f) radiusPower = 3.0f;

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            serverLevel.playSound(null, pos.x, pos.y, pos.z, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        double damageRadius = radiusPower * 2.5;
        AABB damageBox = new AABB(pos.x, pos.y, pos.z, pos.x, pos.y, pos.z).inflate(damageRadius);
        java.util.List<Entity> entities = level.getEntities(source, damageBox);

        DamageSource explosionSource = level.damageSources().explosion(source, owner);

        for (Entity e : entities) {
            if (e instanceof LivingEntity living) {
                if (owner != null && e.equals(owner)) continue;
                double distSqr = e.distanceToSqr(pos);
                if (distSqr <= damageRadius * damageRadius) {
                    float totalDamage = 10.0f + damagePower * 5.0f;
                    if (totalDamage > 50.0f) totalDamage = 50.0f;

                    living.invulnerableTime = 0;
                    living.hurt(explosionSource, totalDamage);
                    living.invulnerableTime = 0;
                }
            }
        }
    }
}
