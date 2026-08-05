package net.xxxjk.TYPE_MOON_WORLD.entity.deadapostle;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.xxxjk.TYPE_MOON_WORLD.combat.deadapostle.NeroChaosRules;

public final class NeroChaosBeastLogic {
   public static final String TAG_OWNER = "NeroChaosBeastOwner";
   public static final String TAG_LIFE_DEBITED = "NeroChaosBeastLifeDebited";
   private static final String TAG_NEXT_ATTACK = "NeroChaosBeastNextAttack";

   private NeroChaosBeastLogic() {
   }

   public enum Kind {
      HOUND(10.0F, 10L, 2.0),
      SERPENT(12.0F, 18L, 2.2),
      STAG(25.0F, 28L, 2.7),
      BIRD(10.0F, 20L, 2.0);

      private final float damage;
      private final long attackCooldown;
      private final double reach;

      Kind(float damage, long attackCooldown, double reach) {
         this.damage = damage;
         this.attackCooldown = attackCooldown;
         this.reach = reach;
      }
   }

   public static boolean isBeast(LivingEntity entity) {
      return entity instanceof NeroChaosHoundEntity
         || entity instanceof NeroChaosSerpentEntity
         || entity instanceof NeroChaosStagEntity
         || entity instanceof NeroChaosBirdEntity;
   }

   public static Kind kind(Mob beast) {
      if (beast instanceof NeroChaosSerpentEntity) return Kind.SERPENT;
      if (beast instanceof NeroChaosStagEntity) return Kind.STAG;
      if (beast instanceof NeroChaosBirdEntity) return Kind.BIRD;
      return Kind.HOUND;
   }

   public static void setOwner(Mob beast, NeroChaosEntity owner) {
      if (owner != null) {
         beast.getPersistentData().putUUID(TAG_OWNER, owner.getUUID());
         beast.setPersistenceRequired();
      }
   }

   public static UUID ownerUuid(Entity beast) {
      CompoundTag data = beast.getPersistentData();
      return data.hasUUID(TAG_OWNER) ? data.getUUID(TAG_OWNER) : null;
   }

   public static NeroChaosEntity owner(Mob beast) {
      UUID uuid = ownerUuid(beast);
      if (uuid == null || !(beast.level() instanceof ServerLevel level)) return null;
      return level.getEntity(uuid) instanceof NeroChaosEntity owner ? owner : null;
   }

   public static boolean isAllied(Entity beast, Entity other) {
      NeroChaosEntity owner = beast instanceof Mob mob ? owner(mob) : null;
      return other == owner || isAlliedToOwner(owner, other);
   }

   public static boolean isAlliedToOwner(NeroChaosEntity owner, Entity other) {
      return owner != null && other instanceof NeroChaosBeastEntityMarker marker
         && owner.getUUID().equals(marker.neroChaosOwnerUuid());
   }

   public static boolean tick(Mob beast) {
      NeroChaosEntity owner = owner(beast);
      long now = beast.level().getGameTime();
      if (beast.tickCount % 5 == 0 && beast.level() instanceof ServerLevel level) {
         level.sendParticles(ParticleTypes.SMOKE, beast.getX(), beast.getY() + beast.getBbHeight() * 0.55,
            beast.getZ(), 2, 0.18, 0.22, 0.18, 0.01);
      }
      if (owner == null) {
         beast.setTarget(null);
         return false;
      }

      LivingEntity target = beast.getTarget();
      if (target == null || !target.isAlive() || isAllied(beast, target)) {
         target = findTarget(beast, owner);
         beast.setTarget(target);
      }

      Kind kind = kind(beast);
      if (target != null) {
         beast.getLookControl().setLookAt(target, 30.0F, 30.0F);
         double distanceSqr = beast.distanceToSqr(target);
         if (kind == Kind.BIRD) {
            flyToward(beast, target);
         } else if (distanceSqr > kind.reach * kind.reach) {
            beast.getNavigation().moveTo(target, kind == Kind.STAG ? 1.05 : 1.25);
         } else {
            beast.getNavigation().stop();
            if (now >= beast.getPersistentData().getLong(TAG_NEXT_ATTACK)) {
               attack(beast, target, kind);
               beast.getPersistentData().putLong(TAG_NEXT_ATTACK, now + kind.attackCooldown);
            }
         }
      } else if (kind == Kind.BIRD) {
         flyToward(beast, owner);
      } else if (beast.distanceToSqr(owner) > 16.0) {
         beast.getNavigation().moveTo(owner, 1.15);
      } else {
         beast.getNavigation().stop();
      }
      return true;
   }

   public static void onDeath(LivingEntity beast) {
      if (beast.getPersistentData().getBoolean(TAG_LIFE_DEBITED)) return;
      beast.getPersistentData().putBoolean(TAG_LIFE_DEBITED, true);
      if (!(beast instanceof Mob mob)) return;
      NeroChaosEntity owner = owner(mob);
      if (owner != null && owner.getRemainingLives() > 0) {
         int remainingLives = NeroChaosRules.consumeLife(owner.getRemainingLives());
         owner.setRemainingLives(remainingLives);
         if (remainingLives <= 0 && owner.isAlive()) {
            owner.kill();
         }
      }
   }

   private static LivingEntity findTarget(Mob beast, NeroChaosEntity owner) {
      return beast.level().getEntitiesOfClass(
            LivingEntity.class,
            owner.getBoundingBox().inflate(48.0),
            target -> target != beast && target != owner && target.isAlive()
               && !isAllied(beast, target)
         ).stream()
         .min((left, right) -> Double.compare(left.distanceToSqr(beast), right.distanceToSqr(beast)))
         .orElse(null);
   }

   private static void attack(Mob beast, LivingEntity target, Kind kind) {
      beast.swing(InteractionHand.MAIN_HAND);
      if (!target.hurt(beast.damageSources().mobAttack(beast), kind.damage)) return;
      if (kind == Kind.SERPENT) {
         target.addEffect(new MobEffectInstance(MobEffects.POISON, 70, 0, false, true, true));
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true, true));
      } else if (kind == Kind.STAG) {
         Vec3 push = target.position().subtract(beast.position()).multiply(1.0, 0.0, 1.0);
         if (push.lengthSqr() > 1.0E-6) {
            push = push.normalize().scale(0.75);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.18, push.z));
            target.hurtMarked = true;
         }
      }
   }

   private static void flyToward(Mob beast, LivingEntity target) {
      Vec3 delta = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(beast.position());
      if (delta.lengthSqr() > 1.0E-6) {
         Vec3 velocity = delta.normalize().scale(0.22);
         beast.setDeltaMovement(velocity);
         beast.hurtMarked = true;
      }
   }

   /**
    * Small adapter implemented by all four beast carriers so ally checks do
    * not depend on a shared vanilla superclass.
    */
   public interface NeroChaosBeastEntityMarker {
      UUID neroChaosOwnerUuid();
   }
}
