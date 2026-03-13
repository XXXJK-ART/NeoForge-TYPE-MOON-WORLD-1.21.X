package net.xxxjk.TYPE_MOON_WORLD.magic.broken_phantasm;

import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.utils.EntityUtils;

public class UBWBrokenPhantasmExplosion {
   public static void explode(Level level, Entity source, Entity owner, ItemStack stack, Vec3 pos) {
      if (!level.isClientSide) {
         double cost = MagicBrokenPhantasm.calculateCost(stack, false);
         float explosionPower = (float)(cost / 20.0);
         if (explosionPower < 2.0F) {
            explosionPower = 2.0F;
         }

         float damagePower = explosionPower;
         float radiusPower = Math.min(explosionPower, 6.0F);
         if (radiusPower < 3.0F) {
            radiusPower = 3.0F;
         }

         if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            serverLevel.sendParticles(ParticleTypes.FLASH, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
            serverLevel.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0F, 1.0F);
         }

         double damageRadius = radiusPower * 2.5;
         AABB damageBox = new AABB(pos.x, pos.y, pos.z, pos.x, pos.y, pos.z).inflate(damageRadius);
         List<Entity> entities = level.getEntities(source, damageBox);
         DamageSource explosionSource = level.damageSources().explosion(source, owner);

         for (Entity e : entities) {
            if (e instanceof LivingEntity living && (owner == null || !e.equals(owner)) && !EntityUtils.isImmunePlayerTarget(e)) {
               double distSqr = e.distanceToSqr(pos);
               if (distSqr <= damageRadius * damageRadius) {
                  float totalDamage = 10.0F + damagePower * 5.0F;
                  if (totalDamage > 50.0F) {
                     totalDamage = 50.0F;
                  }

                  living.invulnerableTime = 0;
                  living.hurt(explosionSource, totalDamage);
                  living.invulnerableTime = 0;
               }
            }
         }
      }
   }
}
